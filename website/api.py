from flask_restful import Resource, reqparse
from .models import LoginDB, Student, Teacher, Teacher_Subject
from . import db

# get locahost/api/info
# Student/Teacher credentials
# get localhost/api/mark
# -> Current location of student
# -> db key search and +1 to db

# http://domain.com/api/login/add -> POST JSON
# Parse recieved JSON that contains
# { id   : [email]
#   pass : [pass]
#   type : [S/T] }
class AuthAdd(Resource):
    def get(self):
        user = LoginDB.query.all()
        return { "users" : f"{user}" }

    def post(self):
        task_post_args = reqparse.RequestParser()
        task_post_args.add_argument("email", type=str, required=True)
        task_post_args.add_argument("pass", type=str, required=True)
        task_post_args.add_argument("login_type", type=str, required=True)

        args = task_post_args.parse_args()

        user = LoginDB.query.filter_by(email=args["email"]).first()
        if user:
            return { "Login": "Exists" }

        auth = LoginDB( email=args["email"],
                        password=args["pass"],
                        login_type=args["login_type"] )
        db.session.add_all([auth])
        db.session.commit()

        return { "Login": "Added" }

    def delete(self):
        task_post_args = reqparse.RequestParser()
        task_post_args.add_argument("email", type=str, required=True)
        task_post_args.add_argument("pass", type=str, required=True)
        task_post_args.add_argument("login_type", type=str, required=True)
        args = task_post_args.parse_args()

        user = LoginDB.query.filter_by(email=args["email"]).first()
        if user:
            db.session.delete(user)
            db.session.commit()
            return { "Login": "Deleted" }
        else:
            return { "Login": "Doesn't Exist" }


# http://domain.com/api/login -> JSON
# Parse recieved JSON that contains
# { id   : [email]
#   pass : [pass]
#   type : [S/T] }
class Authenticate(Resource):
    def post(self):
        task_post_args = reqparse.RequestParser()
        task_post_args.add_argument("email", type=str, required=True)
        task_post_args.add_argument("pass", type=str, required=True)
        task_post_args.add_argument("login_type", type=str, required=True)
        args = task_post_args.parse_args()
        # print(args)

        try:
            user = LoginDB.query.filter_by(email=args["email"].strip()).first()
            if user.login_type == args["login_type"]:
                if user.password == args["pass"].strip():
                    return { 'login': 'success' }

        except AttributeError:
            return { 'login': "404" }

        return { 'login': 'failed' }


# http://localhost:80/api/info -> JSON
# Parse recieved JSON that contains
# { id   : [email]
#   pass : [pass]
#   type : [S/T] }

class GetInfo(Resource):
    def post(self):
        task_post_args = reqparse.RequestParser()
        task_post_args.add_argument("email", type=str, required=True)
        task_post_args.add_argument("pass", type=str, required=True)
        task_post_args.add_argument("login_type", type=str, required=True)

        args = task_post_args.parse_args()
        if args["login_type"].lower() == 't':
            teacher = Teacher.query.filter_by(email=args["email"]).first()
            try:
                if teacher.password == args["pass"]:
                    teacher_subject = Teacher_Subject.query.filter_by(email=args["email"]).first()
                    response = { "response_code" : "200",
                        "name" : f"{teacher.name}",
                        "email": f"{teacher.email}",
                        "subjects": f"{teacher_subject.sub_1},{teacher_subject.sub_2}",
                    }
                    return response
                else:
                        return { "response_code" : "401" }
            except AttributeError:
                    return { "response_code" : "404" }

        elif args["login_type"].lower() == 's':
            user = Student.query.filter_by(email=args["email"]).first()
            try:
                if user.password == args["pass"]:
                    name = f'{user.fname} {user.mname} {user.lname}'
                    return { "response_code": "200",
                        "name" : "{}".format(name),
                        "id" : "{}".format(user.email.split("@")[0]),
                        "course" : "{}".format(user.course),
                        "year" : "{}".format(user.year),
                        "div" : "{}".format(user.div),
                        "batch" : "{}".format(user.batch),
                        "theory_attended" : user.lect_attended,
                        "prac_attended" : user.pract_attended,
                        "theory_total" : user.total_lect,
                        "prac_total" : user.total_pract
                    }
                else:
                    return { "response_code" : "401" }
            except AttributeError:
                return { "response_code" : "404" }

# http://localhost:80/api/mark -> JSON
# Parse recieved JSON that contains
# { email, password, login_type, code, location }
from .mark_attendance import mark_attendance, locationValidation
class MarkAttendance(Resource):
    def post(self):
        task_post_args = reqparse.RequestParser()
        task_post_args.add_argument("email", type=str, required=True)
        task_post_args.add_argument("pass", type=str, required=True)
        task_post_args.add_argument("login_type", type=str, required=True)
        task_post_args.add_argument("code", type=str, required=True)
        task_post_args.add_argument("location", type=str, required=True)

        args = task_post_args.parse_args()
        
        req = { "email" : args["email"], 
                "code" : args["code"],
                "location" : args["location"] }

        if locationValidation(args["location"]):
            return mark_attendance(req)
        else:
            return { "response_code" : "403" }

# http://localhost:80/api/generate -> JSON
# Parse recieved JSON that contains
# { email, password, login_type, subject, lect_time, lect_type, year, div }
from .mark_attendance import generate_attendance_code
class GenerateCode(Resource):
    def post(self):
        task_post_args = reqparse.RequestParser()
        task_post_args.add_argument("email", type=str, required=True)
        task_post_args.add_argument("pass", type=str, required=True)
        task_post_args.add_argument("login_type", type=str, required=True)
        task_post_args.add_argument("subject", type=str, required=True)
        task_post_args.add_argument("lect_time", type=str, required=True)
        task_post_args.add_argument("lect_type", type=str, required=True)
        task_post_args.add_argument("year", type=str, required=True)
        task_post_args.add_argument("div", type=str, required=True)

        args = task_post_args.parse_args()

        if args["login_type"].lower() == 't':
            teacher = Teacher.query.filter_by(email=args["email"]).first()
            if teacher.password != args["pass"]:
                return { "response_code" : "401" }
        elif args["login_type"].lower() == 's':
            return { "response_code" : "401" }

        teacher = Teacher.query.filter_by(email=args["email"]).first()
        req = { "name" : f"{teacher.name}",
                "course" : teacher.course,
                "subject" : args["subject"],
                "lect_time" : args["lect_time"],
                "lect_type" : args["lect_type"],
                "year" : args["year"],
                "div" : args["div"] }

        response = generate_attendance_code(req)

        import json
        from os import path
        codeMapper = {}

        if response["response_code"] == "200":
            file = f'attendance_record\\map-code.json'
            if not path.exists(file):
                with open(file, 'w') as fp:
                    fp.write('{}')

            with open(file, 'r+') as j:
                codeMapper = json.loads(j.read())
                codeMapper.update({response["name"]:response["code"]})
                j.seek(0)
                json.dump(codeMapper, j, indent = 4)

            file = f'attendance_record\\map-class.json'
            if not path.exists(file):
                with open(file, 'w') as fp:
                    fp.write('{}')

            with open(file, 'r+') as j:
                codeMapper = json.loads(j.read())
                codeMapper.update({response["class"]:response["code"]})
                j.seek(0)
                json.dump(codeMapper, j, indent = 4)
            return response
        else:
            return { "response_code" : "401"}

# http://localhost:80/api/remove -> JSON
# Parse recieved JSON that contains
# { email, password, login_type, code }
# remove code from the codeMapper.json
from .mark_attendance import remove_attendance
class RemoveCode(Resource):
    def post(self):
        task_post_args = reqparse.RequestParser()
        task_post_args.add_argument("email", type=str, required=True)
        task_post_args.add_argument("pass", type=str, required=True)
        task_post_args.add_argument("login_type", type=str, required=True)
        task_post_args.add_argument("subject", type=str, required=True)
        task_post_args.add_argument("lect_time", type=str, required=True)
        task_post_args.add_argument("lect_type", type=str, required=True)
        task_post_args.add_argument("year", type=str, required=True)
        task_post_args.add_argument("div", type=str, required=True)

        args = task_post_args.parse_args()

        if args["login_type"].lower() == 't':
            teacher = Teacher.query.filter_by(email=args["email"]).first()
            if teacher.password != args["pass"]:
                return { "response_code" : "401" }
        elif args["login_type"].lower() == 's':
            return { "response_code" : "401" }
        
        teacher = Teacher.query.filter_by(email=args["email"]).first()
        req = { "name" : teacher.name,
                "course" : teacher.course,
                "subject" : args["subject"],
                "lect_time" : args["lect_time"],
                "lect_type" : args["lect_type"],
                "year" : args["year"],
                "div" : args["div"] }
        
        return remove_attendance(req)