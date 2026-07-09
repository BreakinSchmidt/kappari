import json
import hashlib
from kappari.network_client import get_client

c = get_client()
t = c.authenticate(c.config.email, c.config.password)

r1 = c.make_authenticated_request('sync/grocerylists/', t)
lists = r1.json().get('result', [])
list_map = {}
print("Grocery Lists:")
for l in lists:
    uid = l['uid']
    hash_uid = hashlib.sha256(uid.encode('utf-8')).hexdigest().upper()
    hash_uid2 = hashlib.sha256(uid.lower().encode('utf-8')).hexdigest().upper()
    list_map[uid] = l['name']
    print(f"- {l['name']}: {uid}")
    print(f"  (Hash upper: {hash_uid})")
    print(f"  (Hash lower: {hash_uid2})")

r2 = c.make_authenticated_request('sync/groceries/', t)
items = r2.json().get('result', [])
list_uids_in_items = set()
for item in items:
    if 'list_uid' in item and item['list_uid']:
        list_uids_in_items.add(item['list_uid'])

print("\nList UIDs found in items:")
for lu in list_uids_in_items:
    if lu in list_map:
        print(f"{lu} -> {list_map[lu]}")
    else:
        print(f"{lu} -> Unknown")
