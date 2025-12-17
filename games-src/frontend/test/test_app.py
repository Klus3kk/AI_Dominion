import datetime
import io
from typing import IO

import pytest
from flask import session, g, get_flashed_messages

from frontend import create_app
from frontend.app import get_user, set_user
from frontend.service import User


class MockService:
    def __init__(self):
        self.users = {}
        self.submissions = []

    def load_user(self, user: User) -> bool:
        if user.uid in self.users:
            user.team, user.classes = self.users[user.uid]
            return True
        else:
            return False

    def new_submission(self, team: str, stream: IO[bytes]) -> int:
        assert team is not None
        self.submissions.append((team, stream.read()))
        return len(self.submissions)

    def detach_user(self, uid):
        assert uid == 'mykey:666'

    def save_user(self, user: User):
        return

    def get_classes(self):
        return ['L1']

    def get_opponents(self):
        return ['Bilbo', 'Primula']

    def get_team_submissions(self, team):
        return {
            i: {
                'id': i,
                'time': datetime.datetime.now(),
                'status': 'Just fine',
                'tests': None,
                'results': {'Bilbo': [None, None], 'Primula': [None, None]},
                'basedir': 'some/dir',
                'path': 'some/path',
            }
            for i, s in enumerate(self.submissions)
        }


@pytest.fixture
def service():
    return MockService()


@pytest.fixture
def app(service):
    app = create_app()
    app.config['TESTING'] = True
    app.config['DATABASE'] = {}
    del app.config['DEADLINE']
    with app.app_context():
        g.db = None
        g.service = service
        yield app


@pytest.fixture
def client(app):
    return app.test_client()


def test_entry_instructor(client):
    with client:
        resp = client.post('/', data={'roles': 'Instructor', 'oauth_consumer_key': 'mykey', 'user_id': '12345',
                                      'lis_person_name_full': 'My Name',
                                      'lis_person_contact_email_primary': 'email@example.com'})
        assert resp.status_code == 302
        assert '/instructor' in resp.location
        user = get_user()
        assert user.name == 'My Name'
        assert '12345' in user.uid
        assert 'mykey' in user.uid
        assert 'email@example.com' == user.email
        assert user.is_instructor
        assert not user.is_impersonating
        assert user.team is None
        assert user.classes is None


def test_entry_unknown_student(client):
    with client:
        resp = client.post('/', data={'roles': 'Student', 'oauth_consumer_key': 'mykey', 'user_id': '12345',
                                      'lis_person_name_full': 'My Name',
                                      'lis_person_contact_email_primary': 'email@example.com'})
        assert resp.status_code == 302
        assert '/join' in resp.location
        user = get_user()
        assert user.name == 'My Name'
        assert '12345' in user.uid
        assert 'mykey' in user.uid
        assert 'email@example.com' == user.email
        assert not user.is_instructor
        assert not user.is_impersonating
        assert user.team is None
        assert user.classes is None


def test_entry_known_student(client, service):
    service.users['mykey:12345'] = ("My Team", "L666")
    with client:
        resp = client.post('/', data={'roles': 'Student', 'oauth_consumer_key': 'mykey', 'user_id': '12345',
                                      'lis_person_name_full': 'My Name',
                                      'lis_person_contact_email_primary': 'email@example.com'})
        assert resp.status_code == 302
        assert '/student' in resp.location
        user = get_user()
        assert user.name == 'My Name'
        assert '12345' in user.uid
        assert 'mykey' in user.uid
        assert 'email@example.com' == user.email
        assert not user.is_instructor
        assert not user.is_impersonating
        assert user.team == "My Team"
        assert user.classes == "L666"


@pytest.mark.parametrize("endpoint", ["/", "/instructor", "/student"])
def test_entry_no_session_get(client, endpoint):
    resp = client.get(endpoint)
    assert resp.status_code == 401


@pytest.mark.parametrize("endpoint", ["/join", "/student", "/impersonate/blah", "/detach/uid", "/revert"])
def test_entry_no_session_post(client, endpoint):
    resp = client.post(endpoint)
    assert resp.status_code == 401


def test_student_fails_if_no_file1(app, client, service):
    app.config['DEADLINE'] = '2099-12-31T23:59:59+01:00'
    with client.session_transaction() as session:
        set_user(User(uid="mykey:12345", name="My Name", email="<EMAIL>", team="My Team"), _session=session)
    with client:
        resp = client.post('/student')
        assert resp.status_code == 302
        assert len(get_flashed_messages()) > 0
        assert len(service.submissions) == 0


def test_student_fails_if_no_file2(app, client, service):
    app.config['DEADLINE'] = '2099-12-31T23:59:59+01:00'
    with client.session_transaction() as session:
        set_user(User(uid="mykey:12345", name="My Name", email="<EMAIL>", team="My Team"), _session=session)
    with client:
        resp = client.post('/student', data={'sources': (None, '')})
        assert resp.status_code == 302
        assert len(get_flashed_messages()) > 0
        assert len(service.submissions) == 0


def test_student_fails_after_the_deadline(app, client, service):
    app.config['DEADLINE'] = '1999-12-31T23:59:59+01:00'
    with client.session_transaction() as session:
        set_user(User(uid="mykey:12345", name="My Name", email="<EMAIL>", team="My Team"), _session=session)
    with client:
        resp = client.post('/student')
        assert resp.status_code == 302
        assert len(get_flashed_messages()) > 0
        assert len(service.submissions) == 0


def test_student_can_submit(app, client, service):
    app.config['DEADLINE'] = '2099-12-31T23:59:59+01:00'
    with client.session_transaction() as session:
        set_user(User(uid="mykey:12345", name="My Name", email="<EMAIL>", team="My Team", is_impersonating=False),
                 _session=session)
    with client:
        resp = client.post('/student', data={'sources': (io.BytesIO(b'Text'), 'player.java')})
        assert resp.status_code == 302
        assert len(get_flashed_messages()) == 0
        assert len(service.submissions) == 1


def test_impersonator_can_submit(app, client, service):
    app.config['DEADLINE'] = '2099-12-31T23:59:59+01:00'
    with client.session_transaction() as session:
        set_user(User(uid="mykey:12345", name="My Name", email="<EMAIL>", team="My Team", is_impersonating=True),
                 _session=session)
    with client:
        resp = client.post('/student', data={'sources': (io.BytesIO(b'Text'), 'player.java')})
        assert resp.status_code == 302
        assert len(get_flashed_messages()) == 0
        assert len(service.submissions) == 1


def test_student_cannot_detach(client):
    with client.session_transaction() as session:
        set_user(User(uid="mykey:12345", name="My Name", email="<EMAIL>", team="My Team", is_instructor=False),
                 _session=session)
    with client:
        resp = client.post('/detach/mykey:12345')
        assert resp.status_code == 401


def test_instructor_can_detach(client):
    with client.session_transaction() as session:
        set_user(User(uid="mykey:12345", name="My Name", email="<EMAIL>", team="My Team", is_instructor=True),
                 _session=session)
    with client:
        resp = client.post('/detach/mykey:666')
        assert resp.status_code == 302


def test_student_cannot_impersonate(client):
    with client.session_transaction() as session:
        set_user(User(uid="mykey:12345", name="My Name", email="<EMAIL>", team="My Team", is_instructor=False),
                 _session=session)
    with client:
        resp = client.post('/impersonate/My Team')
        assert resp.status_code == 401


def test_cannot_join_with_short_name(client):
    with client.session_transaction() as session:
        set_user(User(uid="mykey:12345", name="My Name", email="<EMAIL>"), _session=session)
    with client:
        resp = client.post('/join', data={'team': 'a', 'classes': 'L14'})
        assert resp.status_code == 302
        assert len(get_flashed_messages()) > 0
        user = get_user()
        assert user.team is None
        assert user.classes is None


def test_can_join(client):
    with client.session_transaction() as session:
        set_user(User(uid="mykey:12345", name="My Name", email="<EMAIL>"), _session=session)
    with client:
        resp = client.post('/join', data={'team': 'Lore Ipsum', 'classes': 'L14'})
        assert resp.status_code == 302
        assert '/student' in resp.location
        assert len(get_flashed_messages()) == 0
        user = get_user()
        assert user.team == 'Lore Ipsum'
        assert user.classes == 'L14'


def test_cannot_join_without_classes(client):
    with client.session_transaction() as session:
        set_user(User(uid="mykey:12345", name="My Name", email="<EMAIL>"), _session=session)
    with client:
        resp = client.post('/join', data={'team': 'Lore Ipsum'})
        assert resp.status_code == 400
        user = get_user()
        assert user.team is None
        assert user.classes is None


def test_cannot_join_with_empty_classes(client):
    with client.session_transaction() as session:
        set_user(User(uid="mykey:12345", name="My Name", email="<EMAIL>"), _session=session)
    with client:
        resp = client.post('/join', data={'team': 'Lore Ipsum', 'classes': ''})
        assert resp.status_code == 302
        assert '/join' in resp.location
        assert len(get_flashed_messages()) > 0
        user = get_user()
        assert user.team is None
        assert user.classes is None


def test_cannot_user_slash_in_team_name(client):
    with client.session_transaction() as session:
        set_user(User(uid="mykey:12345", name="My Name", email="<EMAIL>"), _session=session)
    with client:
        resp = client.post('/join', data={'team': 'Lore/Ipsum', 'classes': 'L14'})
        assert resp.status_code == 302
        assert '/join' in resp.location
        assert len(get_flashed_messages()) > 0
        user = get_user()
        assert user.team is None
        assert user.classes is None


def test_student_workflow(client, app):
    app.config['DEADLINE'] = '2099-12-31T23:59:59+01:00'
    resp = client.post('/', data={'roles': 'Student', 'oauth_consumer_key': 'mykey', 'user_id': '12345',
                                  'lis_person_name_full': 'My Name',
                                  'lis_person_contact_email_primary': 'email@example.com'}, follow_redirects=True)
    assert resp.status_code == 200
    assert 'L1' in resp.get_data(as_text=True)
    resp = client.post('/join', data={'classes': 'L1', 'team': 'My Team'}, follow_redirects=True)
    assert resp.status_code == 200
    assert 'Bilbo' in resp.get_data(as_text=True)
    assert 'Primula' in resp.get_data(as_text=True)
    assert 'Just fine' not in resp.get_data(as_text=True)
    resp = client.post('/student', data={'sources': (io.BytesIO(b'Text'), 'player.java')}, follow_redirects=True)
    assert 'Just fine' in resp.get_data(as_text=True)
