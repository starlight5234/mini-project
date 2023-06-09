from flask import Blueprint, render_template#, redirect, url_for
# from flask_login import current_user
# from .models import Stats, User
# from . import db

views = Blueprint('views', __name__)

@views.route('/home', methods=['GET', 'POST'])
def home():
    return render_template("home.html")

# @views.route('/increment_attendance', methods=['POST'])
# def increment_attendance():
#     # Reset Attendance
#     stat = Stats.query.filter_by(user_email=current_user.email).first()
#     stat.total_attendance = 1
#     db.session.commit()
#     return redirect(url_for('views.home'))

@views.route('/', methods=['GET'])
def root():
    return "online"
