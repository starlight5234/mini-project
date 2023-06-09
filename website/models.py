from . import db

class LoginDB(db.Model):
    email = db.Column(db.String(10), primary_key=True)
    password = db.Column(db.String(), nullable=False)
    login_type = db.Column(db.String(1))

class Student(db.Model):
    email = db.Column(db.String(), primary_key=True) # Max threshold 25
    password = db.Column(db.String(), nullable=False)
    fname = db.Column(db.String())
    mname = db.Column(db.String())
    lname = db.Column(db.String())
    course = db.Column(db.String())
    year = db.Column(db.String())
    div = db.Column(db.String())
    batch = db.Column(db.String())
    lect_attended = db.Column(db.Integer(), nullable=False, default="0")
    pract_attended = db.Column(db.Integer(), nullable=False, default="0")
    total_lect = db.Column(db.Integer(), nullable=False, default="0")
    total_pract = db.Column(db.Integer(), nullable=False, default="0")

class Teacher(db.Model):
    email = db.Column(db.String(), primary_key=True) # Max threshold unknown
    password = db.Column(db.String(), nullable=False)
    name = db.Column(db.String())
    course = db.Column(db.String())

class Teacher_Subject(db.Model):
    id = db.Column(db.Integer(), primary_key=True)
    email = db.Column(db.String(), db.ForeignKey('teacher.email'), unique=True)
    sub_1 = db.Column(db.String())
    sub_1_lect_count = db.Column(db.Integer(), nullable=False)
    sub_2 = db.Column(db.String())
    sub_2_lect_count = db.Column(db.Integer(), nullable=False)
    # To-Do: Add more subjects depending on faculty
    # Can be done by appending number of subjects to a different column and then iterating sub_{num}
