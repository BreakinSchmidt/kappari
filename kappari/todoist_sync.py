import os
import json
import requests
from typing import List, Dict
from pathlib import Path
from . import log

class TodoistSync:
    def __init__(self, token: str):
        self.token = token
        self.headers = {
            "Authorization": f"Bearer {self.token}",
            "Content-Type": "application/json"
        }
        self.state_file = Path("todoist_state.json")
        self.state = self._load_state()
        self.api_base = "https://api.todoist.com/api/v1"
        self._logged_in = bool(self.token)

    def _load_state(self) -> Dict[str, str]:
        if self.state_file.exists():
            try:
                return json.loads(self.state_file.read_text())
            except Exception as e:
                log.error(f"Failed to load state file: {e}")
        return {}

    def _save_state(self):
        try:
            self.state_file.write_text(json.dumps(self.state, indent=2))
        except Exception as e:
            log.error(f"Failed to save state file: {e}")

    def _get_or_create_project(self, project_name: str) -> str:
        res = requests.get(f"{self.api_base}/projects", headers=self.headers)
        res.raise_for_status()
        projects = res.json().get("results", [])
        
        for p in projects:
            if p["name"] == project_name:
                return p["id"]
        
        log.info(f"Creating new Todoist project: {project_name}")
        res = requests.post(f"{self.api_base}/projects", headers=self.headers, json={"name": project_name})
        res.raise_for_status()
        return res.json()["id"]

    def _format_item_name(self, name: str, aisle_name: str) -> str:
        return f"[{aisle_name}] {name}"

    def sync_lists(self, project_name: str, groceries: List[Dict], paprika_client, jwt_token: str):
        if not self._logged_in:
            log.error("Todoist token is missing.")
            return

        try:
            project_id = self._get_or_create_project(project_name)
            
            # Fetch active tasks in the project
            res = requests.get(f"{self.api_base}/tasks", headers=self.headers, params={"project_id": project_id})
            res.raise_for_status()
            active_tasks = {t["id"]: t for t in res.json().get("results", [])}
            
            needs_save = False
            
            # Sync Todoist -> Paprika (check off items)
            for p_item in groceries:
                uid = p_item['uid']
                if not p_item.get('purchased', False):
                    if uid in self.state:
                        task_id = self.state[uid]
                        # If it was in Todoist but is no longer active, it must have been completed (or deleted)
                        if task_id not in active_tasks:
                            expected_name = self._format_item_name(p_item['name'], p_item.get('aisle', ''))
                            log.info(f"Item '{expected_name}' checked off in Todoist. Updating Paprika API.")
                            if paprika_client.mark_grocery_purchased(jwt_token, p_item):
                                p_item['purchased'] = True
                                del self.state[uid]
                                needs_save = True
            
            # Sync Paprika -> Todoist (add new items)
            for p_item in groceries:
                uid = p_item['uid']
                if not p_item.get('purchased', False) and uid not in self.state:
                    formatted_name = self._format_item_name(p_item['name'], p_item.get('aisle', ''))
                    log.info(f"Adding new item to Todoist: {formatted_name}")
                    
                    res = requests.post(
                        f"{self.api_base}/tasks", 
                        headers=self.headers, 
                        json={"content": formatted_name, "project_id": project_id}
                    )
                    
                    if res.status_code == 200 or res.status_code == 201:
                        new_task = res.json()
                        self.state[uid] = new_task["id"]
                        needs_save = True
                    else:
                        log.error(f"Failed to add task {formatted_name} to Todoist: {res.text}")

            if needs_save:
                self._save_state()

        except Exception as e:
            log.error(f"Error during Todoist sync: {e}")
