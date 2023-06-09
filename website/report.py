import fnmatch
from datetime import date
import os
import json

# I/P -> { name }
# O/P -> { date[], subject[], year[], div[], lect_type[], lect_time[] }
def possibleFilters(req: dict):
    try:
        attendance_record = f'{os.getcwd()}\\attendance_record'

        matches = []
        for root, dirnames, filenames in os.walk(attendance_record):
            for filename in fnmatch.filter(filenames, '*.json'):
                matches.append(os.path.join(root, filename))
    

        filters = []
        for x in matches:
            if 'map' not in x:
                if req["name"] in x:
                    filters.append(x.split(attendance_record)[1][1:])

        return filters

        # codeData = f'{req["subject"]}_{req["lect_time"]}_{req["lect_type"]}_{req["year"]}_{req["div"]}'
        # today = date.today()
        # pathToUserDataJSON = f'attendance_record\\{today}\\{req["name"]}\\{req["subject"]}\\{req["year"]}\\{req["div"]}\\{req["lect_type"]}'
    
        # file_name = f'{pathToUserDataJSON}\\userdata-{req["lect_time"]}.json'
        # with open(file_name, 'r') as fp:
        #     userdataJSON = json.loads(fp.read())
            # return userdataJSON
    
    except FileNotFoundError:
        return { "response_code" : "404" }
    
req = { "name" : "Prajakta Khelkar",
                "course" : "Computer",
                "subject" : "SPCC",
                "lect_time" : "8_50",
                "lect_type" : "Th",
                "year" : "TE",
                "div" : "A" }

print(possibleFilters(req))