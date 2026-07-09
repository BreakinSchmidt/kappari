import gzip
import json
from kappari.network_client import get_client

c = get_client()
t = c.authenticate(c.config.email, c.config.password)
r = c.make_authenticated_request('sync/groceries/', t)
items = r.json()['result']
item = [i for i in items if not i['purchased']][0]
print('Original:', item['name'], item['purchased'])

item['purchased'] = True

# Try posting as an array
payload = gzip.compress(json.dumps([item]).encode('utf-8'))
files={'data': ('file', payload)}

res1 = c.post('sync/groceries/', files=files, headers={'Authorization': 'Bearer ' + t})
print('POST groceries (array):', res1.status_code, res1.text)

res2 = c.post('sync/groceryitems/', files=files, headers={'Authorization': 'Bearer ' + t})
print('POST groceryitems (array):', res2.status_code, res2.text)
