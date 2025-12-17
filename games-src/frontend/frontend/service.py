import datetime
import itertools
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Union, IO

import MySQLdb.connections


@dataclass(frozen=True)
class JavaClass:
    package: str
    name: str


def detect_public_class(content: str) -> Union[JavaClass, None]:
    m = re.search(r'public\s+class\s+(\w+)', content, re.IGNORECASE | re.MULTILINE)
    if m is None:
        return None
    name = m.group(1)
    m = re.search(r'package\s+([a-zA-Z0-9_.]+)\s*;', content, re.IGNORECASE | re.MULTILINE)
    if m is None:
        return None
    return JavaClass(m.group(1), name)


@dataclass
class User:
    uid: str
    name: str
    email: str
    is_instructor: bool = False
    is_impersonating: bool = False
    team: Union[str, None] = None
    classes: Union[str, None] = None


class ClassNotFoundError(RuntimeError):
    pass


class FullTeamError(RuntimeError):
    pass


class Service:
    def __init__(self, db: MySQLdb.connections.Connection, max_team_size: int,
                 submissions_dir: Union[str, Path] = 'submissions'):
        self.db = db
        self._opponents = None
        self._classes = None
        self.base_dir = Path(submissions_dir)
        self.max_team_size = max_team_size

    def get_opponents(self):
        if self._opponents is None:
            with self.db.cursor() as cursor:
                cursor.execute('''select distinct team from games_special_submissions order by team''')
                self._opponents = [row[0] for row in cursor.fetchall()]
        return self._opponents

    def get_team_submissions(self, team: str):
        opponents = self.get_opponents()
        submissions = {}
        with self.db.cursor() as cursor:
            cursor.execute(
                '''select id, time, status, tests, mainclass, basedir, path from games_submissions where team=%s order by time desc''',
                (team,))
            for row in cursor.fetchall():
                submissions[row[0]] = {
                    'id': row[0],
                    'time': row[1],
                    'status': row[2],
                    'tests': row[3],
                    'results': {o: [None, None] for o in opponents},
                    'mainclass': row[4],
                    'basedir': row[5],
                    'path': row[6],
                }
            qmarks = ",".join(["%s" for _ in range(len(submissions))])
            ids = list(submissions.keys())
            if len(ids) > 0:
                cursor.execute(
                    f'''(select games_results.second as id, 1 as position, games_special_submissions.team as ours, (result='second') as won, (result='first') as lost, (result='tie') as tie, error from games_results join games_special_submissions where first=games_special_submissions.id and second in ({qmarks})) 
                union all 
                (select games_results.first as id, 0 as position, games_special_submissions.team as ours, (result='first') as won, (result='second') as lost, (result='tie') as tie, error from games_results join games_special_submissions where second=games_special_submissions.id and first in ({qmarks}))''',
                    itertools.chain(ids, ids))
                for row in cursor.fetchall():
                    id, position, opponent, won, lost, tie, error = row
                    assert 0 <= position <= 1
                    submissions[id]['results'][opponent][position] = {
                        'won': bool(won),
                        'lost': bool(lost),
                        'tie': bool(tie),
                        'error': error
                    }
        return submissions

    def get_classes(self):
        if self._classes is None:
            with self.db.cursor() as cursor:
                cursor.execute('''select * from games_classes''')
                self._classes = [row[0] for row in cursor.fetchall()]
        return self._classes

    def get_newest_submissions(self) -> list:
        submissions = []
        with self.db.cursor() as cursor:
            cursor.execute('''select team,
                    (select group_concat(name) from games_users as u where classes is not null and u.team=x.team group by team)
                    ,id,time,status 
                    from games_submissions as x 
                    where id=(select id from games_submissions as y where x.team=y.team order by time desc limit 1)''')
            for row in cursor.fetchall():
                submissions.append({
                    'team': row[0],
                    'students': row[1],
                    'id': row[2],
                    'time': row[3],
                    'status': row[4],
                })
        return submissions

    def get_students(self):
        students = []
        with self.db.cursor() as cursor:
            cursor.execute('''select uid,name,email,games_users.team,classes,count(id) 
                    from games_users left join games_submissions on games_users.team=games_submissions.team 
                    where classes is not null group by uid order by classes,name''')
            for row in cursor.fetchall():
                students.append({
                    'uid': row[0],
                    'name': row[1],
                    'email': row[2],
                    'team': row[3],
                    'classes': row[4],
                    'submissions': row[5],
                })
        return students

    def load_user(self, user: User) -> bool:
        with self.db.cursor() as cursor:
            cursor.execute('''select team,classes from games_users where uid=%s''', (user.uid,))
            row = cursor.fetchone()
            if row is not None:
                user.team = row[0]
                user.classes = row[1]
                return True
            else:
                return False

    def save_user(self, user: User):
        assert not user.is_impersonating
        with self.db.cursor() as cursor:
            cursor.execute('''select count(uid) from games_users where team=%s''', (user.team,))
            row = cursor.fetchone()
            if row is not None and int(row[0]) >= self.max_team_size:
                raise FullTeamError()
            # TODO games_user should reference games_classes in the db
            cursor.execute('''insert into games_users (uid, name, email, team, classes) values (%s, %s, %s, %s, %s)
            on duplicate key update name=%s, email=%s, team=%s, classes=%s''',
                           (user.uid, user.name, user.email, user.team, user.classes, user.name, user.email, user.team,
                            user.classes))
            self.db.commit()

    def new_submission(self, team: str, stream: IO[bytes]) -> int:
        content = stream.read().decode('utf-8')
        class_name = detect_public_class(content)
        if class_name is None:
            raise ClassNotFoundError()
        with self.db.cursor() as cursor:
            cursor.execute('''insert into games_submissions (team, status) values(%s,'Upload')''', (team,))
            cursor.execute('''select last_insert_id()''')
            row = cursor.fetchone()
            assert row is not None
            submission_id = row[0]
            submission_base_dir = self.base_dir / str(submission_id)
            target_dir = submission_base_dir / (class_name.package.replace('.', '/'))
            # TODO double check: target_dir must be a subdirectory of self.base_dir / submission_id
            target_dir.mkdir(parents=True, exist_ok=True)
            target_file = target_dir / f"{class_name.name}.java"
            assert not target_file.exists()
            with target_file.open(mode='wt') as f:
                f.write(content)
            assert target_file.exists()
            main_class = f"{class_name.package}.{class_name.name}"
            cursor.execute(
                '''update games_submissions set path=%s,status='Saved',basedir=%s,mainclass=%s where id=%s''',
                (target_file, submission_base_dir, main_class, submission_id))
        self.db.commit()
        return submission_id

    def detach_user(self, uid: str):
        with self.db.cursor() as cursor:
            cursor.execute('''update games_users set team=NULL,classes=NULL where uid=%s''', (uid,))
            self.db.commit()
