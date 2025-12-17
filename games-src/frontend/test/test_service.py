import datetime
import io
import time
import uuid
from pathlib import Path
from tempfile import TemporaryDirectory

import MySQLdb
import pytest
from testcontainers.mysql import MySqlContainer

from frontend.service import Service, User, FullTeamError

ddl = '''
CREATE TABLE `games_classes` (
  `id` varchar(64) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;


CREATE TABLE `games_users` (
  `uid` varchar(64) NOT NULL,
  `name` varchar(256) NOT NULL,
  `email` varchar(256) NOT NULL,
  `team` varchar(64) DEFAULT NULL,
  `classes` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `games_special_submissions` (
  `id` int(11) NOT NULL DEFAULT '0',
  `team` varchar(64) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `games_submissions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `team` varchar(64) CHARACTER SET latin1 NOT NULL,
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `path` varchar(1024) CHARACTER SET latin1 DEFAULT NULL,
  `status` text,
  `tests` tinyint(1) DEFAULT NULL,
  `mainclass` varchar(255) CHARACTER SET latin1 DEFAULT NULL,
  `basedir` varchar(255) CHARACTER SET latin1 DEFAULT NULL,
  `jarfile` varchar(255) CHARACTER SET latin1 DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10314 DEFAULT CHARSET=utf8;

CREATE TABLE `games_results` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `first` int(11) NOT NULL,
  `second` int(11) NOT NULL,
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `result` enum('tie','first','second') DEFAULT NULL,
  `logfile` varchar(255) DEFAULT NULL,
  `output` varchar(2048) DEFAULT NULL,
  `retval` int(11) DEFAULT NULL,
  `error` varchar(2048) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `first` (`first`),
  KEY `second` (`second`),
  CONSTRAINT `games_results_ibfk_1` FOREIGN KEY (`first`) REFERENCES `games_submissions` (`id`),
  CONSTRAINT `games_results_ibfk_2` FOREIGN KEY (`second`) REFERENCES `games_submissions` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=103924 DEFAULT CHARSET=utf8;

CREATE TABLE `games_tournaments` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `result_id` int(11) NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `result_id` (`result_id`),
  CONSTRAINT `games_tournaments_ibfk_1` FOREIGN KEY (`result_id`) REFERENCES `games_results` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
'''


@pytest.fixture(scope="module")
def container():
    with MySqlContainer(username="root") as c:
        yield c


def connect(container):
    return MySQLdb.connect(host="127.0.0.1", port=int(container.get_exposed_port(container.port)),
                           user=container.username, passwd=container.password, db=container.dbname)


@pytest.fixture()
def db(container):
    dbname = str(uuid.uuid4()).replace("-", "")
    with connect(container) as conn:
        conn.autocommit = True
        with conn.cursor() as cursor:
            cursor.execute(f'''CREATE DATABASE `{dbname}`''')
    container.dbname = dbname
    with connect(container) as conn:
        conn.autocommit = True
        with conn.cursor() as cursor:
            cursor.execute(ddl)
            cursor.execute('''insert into games_classes values('L1'),('L2'),('L3'),('L4')''')
            cursor.execute("""insert into games_special_submissions values
            ('2169','Frodo'),('2168','Meriadoc'),('2167','Peregrin'),('2166','Samwise')            
            """)
            conn.commit()
    with connect(container) as conn:
        conn.autocommit = False
        yield conn


@pytest.fixture()
def service(db):
    with TemporaryDirectory() as tmpdir:
        yield Service(db, 2, tmpdir)


def test_get_classes(service):
    assert set(service.get_classes()) == {'L1', 'L2', 'L3', 'L4'}


def test_get_opponents(service):
    assert set(service.get_opponents()) == {'Frodo', 'Meriadoc', 'Peregrin', 'Samwise'}


def test_save_load_user(service):
    user = User(uid=str(uuid.uuid4()), name="Test", email="test@example.com", team="Team", classes="L1")
    service.save_user(user)
    new_user = User(uid=user.uid, name="Test", email="test@example.com")
    service.load_user(new_user)
    assert user == new_user


def test_save_list(service):
    uid = str(uuid.uuid4())
    service.save_user(User(uid=uid, name="Test", email="test@example.com", team="Team", classes="L1"))
    students = service.get_students()
    assert len(students) == 1
    assert students[0]['uid'] == uid
    assert students[0]['name'] == 'Test'
    assert students[0]['email'] == 'test@example.com'
    assert students[0]['team'] == 'Team'
    assert students[0]['classes'] == 'L1'


def test_save_save_list(service):
    uid = str(uuid.uuid4())
    service.save_user(User(uid=uid, name="Not Test", email="blah@example.com", team="OtherTeam", classes="L4"))
    service.save_user(User(uid=uid, name="Test", email="test@example.com", team="Team", classes="L1"))
    students = service.get_students()
    assert len(students) == 1
    assert students[0]['uid'] == uid
    assert students[0]['name'] == 'Test'
    assert students[0]['email'] == 'test@example.com'
    assert students[0]['team'] == 'Team'
    assert students[0]['classes'] == 'L1'


naive_player = """/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package put.ai.games.naiveplayer;

import java.util.List;
import java.util.Random;
import put.ai.games.game.Board;
import put.ai.games.game.Move;
import put.ai.games.game.Player;

public class NaivePlayer extends Player {

    private Random random = new Random(0xdeadbeef);


    @Override
    public String getName() {
        return "Gracz Naiwny 84868";
    }


    @Override
    public Move nextMove(Board b) {
        List<Move> moves = b.getMovesFor(getColor());
        return moves.get(random.nextInt(moves.size()));
    }
}
"""


def test_submit(service):
    user = User(uid=str(uuid.uuid4()), name="Test", email="test@example.com", team="Team", classes="L1")
    assert len(service.get_team_submissions("Team")) == 0
    sid = service.new_submission(user.team, io.BytesIO(naive_player.encode('utf-8')))
    submissions = service.get_team_submissions("Team")
    assert len(submissions) == 1
    assert set(submissions.keys()) == {sid}
    s = submissions[sid]
    assert s['id'] == sid
    assert s['time'] is not None
    assert datetime.datetime.now(datetime.UTC) - s['time'].replace(tzinfo=datetime.UTC) < datetime.timedelta(minutes=1)
    assert s['status'] == 'Saved'
    assert s['tests'] is None
    assert s['mainclass'] == 'put.ai.games.naiveplayer.NaivePlayer'
    assert s['basedir'] is not None
    assert s['path'] is not None
    assert s['results'] == {'Frodo': [None, None], 'Meriadoc': [None, None], 'Samwise': [None, None],
                            'Peregrin': [None, None]}
    basedir = Path(s['basedir'])
    assert basedir.exists() and basedir.is_dir()
    file = basedir / 'put/ai/games/naiveplayer/NaivePlayer.java'
    assert file.exists()
    assert file.samefile(s['path'])
    with open(file, 'rt') as f:
        content = f.read()
    assert content == naive_player


def test_submit_twice_get_newest(service):
    user = User(uid=str(uuid.uuid4()), name="Test", email="test@example.com", team="Team", classes="L1")
    assert len(service.get_team_submissions("Team")) == 0
    sid1 = service.new_submission(user.team, io.BytesIO(naive_player.encode('utf-8')))
    time.sleep(1)
    sid2 = service.new_submission(user.team, io.BytesIO(naive_player.encode('utf-8')))
    submissions = service.get_team_submissions("Team")
    assert len(submissions) == 2
    assert submissions.keys() == {sid1, sid2}
    newest = service.get_newest_submissions()
    assert len(newest) == 1
    assert newest[0]['id'] == sid2


def test_third_user_cannot_join(service):
    service.save_user(User(uid=str(uuid.uuid4()), name="Test", email="test@example.com", team="Team", classes="L1"))
    service.save_user(User(uid=str(uuid.uuid4()), name="Test", email="test@example.com", team="Team", classes="L1"))
    with pytest.raises(FullTeamError):
        service.save_user(User(uid=str(uuid.uuid4()), name="Test", email="test@example.com", team="Team", classes="L1"))


def test_detach(service):
    uid = str(uuid.uuid4())
    user = User(uid=uid, name="Test", email="test@example.com", team="Team", classes="L1")
    service.save_user(user)
    service.detach_user(uid)
    assert service.load_user(user)
    assert user.team is None
    assert user.classes is None
