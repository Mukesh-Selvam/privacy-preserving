import json
import urllib.request
import urllib.error


def post(url, body):
    data = json.dumps(body).encode('utf-8')
    headers = {'Content-Type': 'application/json'}
    req = urllib.request.Request(url, data=data, headers=headers, method='POST')
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, resp.read().decode('utf-8')
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8')


def get(url):
    try:
        with urllib.request.urlopen(url) as resp:
            return resp.status, resp.read().decode('utf-8')
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8')


if __name__ == '__main__':
    consent_url = 'http://localhost:8080/api/consent'
    patient_url = 'http://localhost:8080/api/consent/1'
    gateway_url = 'http://localhost:8080/api/gateway/patient/1?orgId=insurer-partner&userId=test-demo'
    body = {'patientId': 1, 'field': 'disease', 'consentGiven': False}
    status, body_text = post(consent_url, body)
    print('POST', status)
    print(body_text)
    print('---')
    status, body_text = get(patient_url)
    print('GET', patient_url, status)
    print(body_text)
    print('---')
    status, body_text = get(gateway_url)
    print('GET', gateway_url, status)
    print(body_text)
