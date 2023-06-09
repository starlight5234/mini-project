from website import create_app

ip_addr = '0.0.0.0'

app = create_app()

if __name__ == '__main__':
    app.run(debug=True, host=ip_addr, port=80)