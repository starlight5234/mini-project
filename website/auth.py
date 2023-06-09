from flask import Blueprint, render_template, request, flash, redirect, url_for
from .models import User, Stats
from . import db

from flask_login import login_user, login_required, logout_user

auth = Blueprint('auth', __name__)

@auth.route('/login', methods=['GET', 'POST'])
def login():
    data=request.form
    print(data)

    if request.method == 'POST':
        email = request.form.get('email')

        user = User.query.filter_by(email=email).first()
        password = request.form.get('password')

        if user:
            if password == user.password:
                flash('Logged In', category='success')
                login_user(user, remember=True)
                return redirect(url_for('views.home'))
            else:
                flash('Incorrect password', category='error')
        else:
            flash('Email not registered!', category='error')
        

    return render_template("login.html")

@auth.route('/logout')
@login_required
def logout():
    logout_user()
    return redirect(url_for('auth.login'))

@auth.route('/signup', methods=['GET', 'POST'])
def signup():
    data=request.form
    print(data)
    if request.method == 'POST':
        email = request.form.get('email')
        first_name = request.form.get('firstName')
        last_name = request.form.get('lastName')
        password1 = request.form.get('password1')
        password2 = request.form.get('password2')
        year = request.form.get('year')

        user = User.query.filter_by(email=email).first()

        if user:
            flash('User already exists', category='error')
        elif "@pvppcoe.ac.in" not in email:
            flash('Email not valid', category='error')
        elif len(first_name) < 2:
            flash('First name must be greater than 1 character.', category='error')
        elif len(password1) < 6:
            flash('Password must be at least 6 characters.', category='error')
        elif password1 != password2:
            flash('Password does not match.', category='error')
        elif year not in ['FE', 'SE', 'TE', 'BE']:
            flash('Invalid Year', category='error')
        else:
            new_user = User(email=email, fname=first_name, lname=last_name, year=year, password=password1)
            user_stat = Stats(user_email=new_user.email, total_attendance=1)
            db.session.add_all([new_user, user_stat])
            db.session.commit()

            flash('Account created!', category='success')
            login_user(new_user, remember=True)
            return redirect(url_for('views.home'))

    return render_template("signup.html")
