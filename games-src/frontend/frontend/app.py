import dataclasses
import json
# Something funny going on and PyCharm claims re is unused
# noinspection PyUnresolvedReferences
import re

import MySQLdb.connections
from flask import Flask, request, abort, session, redirect, render_template, g, current_app, flash, url_for
from werkzeug.middleware.proxy_fix import ProxyFix

from .oauth import validate_signature
from .service import *


def get_user() -> User:
    user = session.get('user', None)
    if user is None:
        abort(401)
    return User(**json.loads(user))


def set_user(user: User, _session=None):
    if _session is None:
        _session = session
    _session['user'] = json.dumps(dataclasses.asdict(user))


def get_service():
    # TODO connection pooling
    if 'db' not in g:
        g.db = MySQLdb.connect(**current_app.config['DATABASE'])
        g.db.autocommit = False
    if 'service' not in g:
        g.service = Service(g.db, int(current_app.config['MAX_TEAM_SIZE']))
    return g.service


def close_db(e=None):
    db = g.pop('db', None)
    if db is not None:
        db.close()


def create_app():
    app = Flask(__name__, template_folder='templates', static_folder='static')
    app.wsgi_app = ProxyFix(app.wsgi_app, x_for=1, x_host=1, x_proto=1, x_port=1, x_prefix=1)
    app.config.from_object('frontend.config')
    app.teardown_appcontext(close_db)

    @app.route('/', methods=['GET', 'POST'])
    def entry():
        if request.method == 'POST':
            if not current_app.config['TESTING']:
                validate_signature(current_app.config['CLIENT_KEYS'])
            data = request.form
            uid = f"{data['oauth_consumer_key']}:{data['user_id']}"
            # TODO it seems one can have multiple roles and then this will fail?
            is_instructor = data['roles'].lower() == 'instructor'
            name = data['lis_person_name_full']
            email = data['lis_person_contact_email_primary']
            user = User(uid=uid, is_instructor=is_instructor, name=name, email=email)
            get_service().load_user(user)
            set_user(user)
        else:
            assert request.method == 'GET'
            user = get_user()
        if True and user.is_instructor:
            return redirect(url_for('instructor'))
        elif user.team is None:
            return redirect(url_for('join'))
        else:
            return redirect(url_for('student'))

    @app.route('/student', methods=['GET', 'POST'])
    def student():
        user = get_user()
        assert user.team is not None
        if request.method == 'GET':
            opponents = get_service().get_opponents()
            submissions = get_service().get_team_submissions(user.team)
            return render_template("team.html", user=user, opponents=opponents, submissions=submissions.values())
        else:
            # TODO remove hardcoded strings
            assert request.method == 'POST'
            deadline = datetime.datetime.fromisoformat(current_app.config['DEADLINE'])
            if datetime.datetime.now(deadline.tzinfo) >= deadline:
                flash('Deadline exceeded')
                return redirect(url_for('student'))
            if 'sources' not in request.files:
                flash("Nie przesłano pliku")
                return redirect(url_for('student'))
            file = request.files['sources']
            if len(file.filename) == 0:
                flash("Nie przesłano pliku")
                return redirect(url_for('student'))
            assert user.team is not None
            try:
                get_service().new_submission(user.team, file.stream)
            except ClassNotFoundError:
                flash(
                    "Nie można ustalić nazwy publicznej klasy w zgłoszonym pliku. Zadbaj, żeby główna klasa była zadeklarowana w następujący sposób: <code>public class Klasa</code> i żeby należała do jakiegoś pakietu: <code>package moj.pakiet;</code>.")
            return redirect(url_for('student'))

    @app.route("/revert", methods=['POST'])
    def revert():
        user = get_user()
        if not user.is_instructor:
            abort(401)
        user.is_impersonating = False
        set_user(user)
        return redirect(url_for('instructor'))

    @app.route('/impersonate/<team>', methods=['POST'])
    def impersonate(team):
        user = get_user()
        if not user.is_instructor:
            abort(401)
        user.team = team
        user.is_impersonating = True
        set_user(user)
        return redirect(url_for('student'))

    @app.route('/detach/<uid>', methods=['POST'])
    def detach(uid):
        user = get_user()
        if not user.is_instructor:
            abort(401)
        get_service().detach_user(uid)
        return redirect(url_for('instructor'))

    @app.route('/join', methods=['GET', 'POST'])
    def join():
        user = get_user()
        if request.method == 'POST':
            if user.is_impersonating:
                abort(400)
            team = request.form['team']
            if re.fullmatch(r'^(\w| ){5,}$', team) is None:
                flash('Nazwa drużyny musi mieć przynajmniej 5 znaków ([a-zA-Z0-9_] oraz spacje)')
                return redirect(url_for('join'))
            classes = request.form['classes']
            if len(classes) == 0:
                flash('Musisz wybrać grupę zajęciową')
                return redirect(url_for('join'))
            user.team = team
            user.classes = classes
            try:
                get_service().save_user(user)
                set_user(user)
                return redirect(url_for('student'))
            except FullTeamError:
                # TODO remove hardcoded string
                flash('Drużyna liczy maksymalną liczbę członków')
                return redirect(url_for('join'))
        else:
            classes = get_service().get_classes()
            return render_template('join.html', classes=classes)

    @app.route('/instructor', methods=['GET'])
    def instructor():
        user = get_user()
        if not user.is_instructor:
            abort(401)
        students = get_service().get_students()
        submissions = get_service().get_newest_submissions()
        return render_template("instructor.html", user=user, students=students, submissions=submissions)

    return app


if __name__ == '__main__':
    create_app().run(debug=True)
