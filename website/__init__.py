from pathlib import Path
import os
from flask import Flask
from flask_sqlalchemy import SQLAlchemy
from flask_restful import Api

MAIN_DB = "attendance_app.db"

db = SQLAlchemy()

def create_app():
    app = Flask(__name__)
    app.config['SECRET_KEY'] = 'Clowns'
    app.config['SQLALCHEMY_DATABASE_URI'] = f'sqlite:///../db/{MAIN_DB}'

    from .views import views
    app.register_blueprint(views, url_prefix='/')

    from .api import Authenticate, AuthAdd, GetInfo, MarkAttendance, GenerateCode, RemoveCode

    API = Api(app)
    API.add_resource(Authenticate, '/api/login')
    API.add_resource(AuthAdd, '/api/login/add')
    API.add_resource(GetInfo, '/api/info')
    API.add_resource(MarkAttendance, '/api/mark')
    API.add_resource(GenerateCode, '/api/generate')
    API.add_resource(RemoveCode, '/api/remove')

    db.init_app(app=app)
    with app.app_context():
        if not os.path.exists(f'db/{MAIN_DB}'):
            Path('db/').mkdir(parents=True, exist_ok=True)
            db.create_all()
            print('Created DB')

    return app