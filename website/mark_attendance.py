from pathlib import Path
from datetime import date
from os import path
import json
from .models import Student, Teacher

def locationValidation(locationString: str):
    # Validate location
    from shapely.geometry import Point
    from shapely.geometry.polygon import Polygon

    x,z = locationString.split(',')
    point = Point(x, z)
    polygon = Polygon([(19.050574, 72.878407), (19.050505, 72.878640), (19.050357, 72.878287), (19.050213, 72.878452)])
    return polygon.contains(point)

def mark_attendance(req: dict) -> dict:
    # Student enters code
    # json contains
    # I/P : { email, code, location }
    #
    # Sub_30_03_23 name/extra contains email
    # if contains O/P { attendance marked }
    # else check location within campus or not
    # if within campus then append email to name
    # else error
    codeMapper = {}

    try:
        file = f'attendance_record\\map-code.json'
        with open(file, 'r+') as j:
            codeMapper = json.loads(j.read())

        # list out keys and values separately
        key_list = list(codeMapper.keys())
        val_list = list(codeMapper.values())
        position = val_list.index(req["code"])
        teacher = key_list[position]

        today = date.today()

        file = f'attendance_record\\map-class.json'
        with open(file, 'r+') as j:
            codeMapper = json.loads(j.read())

        # list out keys and values separately
        key_list = list(codeMapper.keys())
        val_list = list(codeMapper.values())
        position = val_list.index(req["code"])

        codeSplit = key_list[position].split('_')
        subject = codeSplit[0]
        lectTime = f'{codeSplit[1]}_{codeSplit[2]}'
        lectType = codeSplit[3]
        year = codeSplit[4]
        div = codeSplit[5]

        pathToUserDataJSON = f'attendance_record\\{today}\\{teacher}\\{subject}\\{year}\\{div}\\{lectType}'
        userdata = f'{pathToUserDataJSON}\\userdata-{lectTime}.json'

        if not path.exists(userdata):
            raise KeyError
        else:
            with open(userdata, 'r+') as userdataFile:
                userdataJSON = json.loads(userdataFile.read())
                data = userdataJSON["email"]
                if f"{req['email'].split('@')[0]}" not in data.split(','):
                    if len(data) != 0:
                        data += f",{req['email'].split('@')[0]}"
                    else:
                        data += f"{req['email'].split('@')[0]}"
                else:
                    return { "response_code" : "208" }

                # print(data)
                userdataJSON["email"] = data
                userdataFile.seek(0)
                json.dump(userdataJSON, userdataFile, indent = 4)

    except (KeyError, FileNotFoundError, ValueError, NameError):
        return { "response_code" : "401"}

    # Increment attendance of the student in DB
    student = Student.query.filter_by(email=req["email"]).first()

    if '-' in lectType:
        student.pract_attended += 1
    else:
        student.lect_attended += 1

    from . import db
    db.session.commit()
    return { "response_code": "200" }

def generate_attendance_code(req: dict) -> dict:
    # Sub_30_03_23
    # subject -> Frontend - Drop down list : Via Backend DB
    # lect_time -> Frontend - Drop down list
    # lect_type -> Frontend - Drop down list ( theory, practical )
    # json contains
    # I/P { email, subject, lect_time, lect_type }
    #
    # Update student.total_lect column for ALL students
    # subject == db.column name + '_lect_count'
    # {Sys.Date}/{Teacher Name}/{Subject}/{Year}/{Div}/{Th-Pr}/userdata-8_50.json
    # {Sys.Date}/{Teacher Name}/{Subject}/{Year}/{Div}/{Th-Pr}/userdata-1_00.json
    # Display the above json to teachers for report -> FRONTEND

    # print('Generated Code')
    codeData = f'{req["subject"]}_{req["lect_time"]}_{req["lect_type"]}_{req["year"]}_{req["div"]}'
    codeString = ""
    import random, string

    codeString = ''.join(random.sample(string.ascii_lowercase, 4))

    pathToUserDataJSON = None

    today = date.today()

    pathToUserDataJSON = f'attendance_record\\{today}\\{req["name"]}\\{req["subject"]}\\{req["year"]}\\{req["div"]}\\{req["lect_type"]}'

    Path(pathToUserDataJSON).mkdir(parents=True, exist_ok=True)

    file_name = f'{pathToUserDataJSON}\\userdata-{req["lect_time"]}.json'

    # Need to filter students based on course, year, div && batch (if practicals)
    students = Student.query.filter_by(course=req["course"], year=req["year"], div=req["div"]).all()

    if not path.exists(file_name):
        with open(file_name, 'w') as fp:
            fp.write('{ "email" : "", "extra" : "" }')
            # Increment total in those students only who's lecture is detected
            if '-' in req["lect_type"]:
                for i in students:
                    if req["lect_type"].split('-')[1] == i.batch:
                        i.total_pract += 1
            else:
                for i in students:
                    i.total_lect += 1

    from . import db
    db.session.commit()

    return { "response_code": "200",
         "code" : f"{codeString}",
         "class" : f"{codeData}",
         "name" :f'{req["name"]}' }

def remove_attendance(req: dict):
    # Remove code from map-class.json and map-code.json if they exist
    # If they don't exist return 400
    # If they exist but no code in them then 404

    codeData = f'{req["subject"]}_{req["lect_time"]}_{req["lect_type"]}_{req["year"]}_{req["div"]}'

    try:
        file = f'attendance_record\\map-class.json'
        with open(file, 'r+') as j:
            codeMapper = json.loads(j.read())
            codeMapper.pop(codeData)
            j.seek(0)
            j.truncate(0)
            json.dump(codeMapper, j, indent = 4)
        
        file = f'attendance_record\\map-code.json'
        with open(file, 'r+') as j:
            codeMapper = json.loads(j.read())
            codeMapper.pop(req["name"])
            j.seek(0)
            j.truncate(0)
            json.dump(codeMapper, j, indent = 4)

    except FileNotFoundError:
        return { "response_code" : "400" }
    except KeyError:
        return { "response_code" : "404" }

    return { "response_code" : "200" }
