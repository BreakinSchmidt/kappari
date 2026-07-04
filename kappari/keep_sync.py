import gkeepapi
from typing import List, Dict
from . import log

class KeepSync:
    def __init__(self, email: str, app_password: str):
        self.email = email
        self.app_password = app_password
        self.keep = gkeepapi.Keep()
        self._logged_in = False

    def login(self) -> bool:
        """Login to Google Keep using an App Password."""
        if not self.email or not self.app_password:
            log.error("Google Keep credentials are missing.")
            return False
            
        try:
            self.keep.login(self.email, self.app_password)
            self._logged_in = True
            log.info("Successfully logged into Google Keep.")
            return True
        except gkeepapi.exception.LoginException as e:
            log.error(f"Failed to login to Google Keep. Check your App Password. Error: {e}")
            return False
        except Exception as e:
            log.error(f"Unexpected error during Keep login: {e}")
            return False

    def _get_or_create_list(self, title: str):
        """Find a Keep list by title, or create it if it doesn't exist."""
        for note in self.keep.all():
            if note.title == title and isinstance(note, gkeepapi.node.List):
                return note
        
        log.info(f"Creating new Google Keep list: {title}")
        return self.keep.createList(title)

    def _format_item_name(self, name: str, aisle_name: str) -> str:
        """Format the item name according to the Option A approach."""
        return f"[{aisle_name}] {name}"

    def sync_lists(self, list_titles: List[str], groceries: List[Dict], db_client):
        """
        Synchronize groceries with Google Keep lists.
        - Adds new unpurchased Paprika groceries to Keep.
        - Marks Paprika items as purchased if checked off in Keep.
        """
        if not self._logged_in:
            if not self.login():
                return

        # Fetch current state from Keep
        self.keep.sync()

        # Build mapping of Keep list objects
        keep_lists = [self._get_or_create_list(title) for title in list_titles]

        # Sync Keep -> Paprika (check off items)
        for keep_list in keep_lists:
            for keep_item in keep_list.items:
                if keep_item.checked:
                    # Find corresponding Paprika item
                    for p_item in groceries:
                        if not p_item['purchased']:
                            expected_name = self._format_item_name(p_item['name'], p_item['aisle_name'])
                            if keep_item.text == expected_name:
                                log.info(f"Item '{expected_name}' checked off in Keep. Updating Paprika DB.")
                                if db_client.mark_grocery_purchased(p_item['uid']):
                                    p_item['purchased'] = True

        # Sync Paprika -> Keep (add new items)
        # Add to the first configured list by default
        default_keep_list = keep_lists[0] if keep_lists else None
        
        if default_keep_list:
            # Get existing unchecked items in Keep to avoid duplicates
            existing_keep_items = set(
                item.text for kl in keep_lists for item in kl.items if not item.checked
            )

            needs_sync = False
            for p_item in groceries:
                if not p_item['purchased']:
                    formatted_name = self._format_item_name(p_item['name'], p_item['aisle_name'])
                    if formatted_name not in existing_keep_items:
                        log.info(f"Adding new item to Keep: {formatted_name}")
                        default_keep_list.add(formatted_name, False)
                        needs_sync = True

            if needs_sync:
                self.keep.sync()
