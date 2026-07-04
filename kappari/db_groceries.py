import hashlib
import sqlite3
import uuid
from typing import Dict, List, Optional
from . import log

def generate_sync_hash() -> str:
    """Generate a new sync hash (64 hex characters) for Paprika synchronization."""
    guid = str(uuid.uuid4()).upper()
    hash_bytes = hashlib.sha256(guid.encode('utf-8')).digest()
    return hash_bytes.hex().upper()

class GroceriesDB:
    def __init__(self, db_path: str):
        self.db_path = db_path

    def _get_purchased_column(self, cursor) -> str:
        cursor.execute("PRAGMA table_info(groceries)")
        columns = [row[1] for row in cursor.fetchall()]
        for col in ['purchased', 'checked', 'is_purchased']:
            if col in columns:
                return col
        return 'purchased' # fallback

    def get_groceries(self) -> List[Dict]:
        """Fetch all groceries joined with their aisle names."""
        if not self.db_path:
            log.warning("No database path configured for GroceriesDB.")
            return []

        try:
            conn = sqlite3.connect(self.db_path)
            conn.row_factory = sqlite3.Row
            cursor = conn.cursor()

            purchased_col = self._get_purchased_column(cursor)

            query = f"""
                SELECT 
                    g.uid, 
                    g.name, 
                    g.{purchased_col} as purchased, 
                    a.name as aisle_name
                FROM groceries g
                LEFT JOIN grocery_aisles a ON g.aisle_uid = a.uid
            """
            cursor.execute(query)
            
            results = []
            for row in cursor.fetchall():
                results.append({
                    "uid": row["uid"],
                    "name": row["name"],
                    "purchased": bool(row["purchased"]),
                    "aisle_name": row["aisle_name"] or "Uncategorized"
                })
                
            conn.close()
            return results
        except Exception as e:
            log.error(f"Failed to fetch groceries: {e}")
            return []

    def mark_grocery_purchased(self, uid: str) -> bool:
        """Mark a grocery item as purchased and update sync fields."""
        if not self.db_path:
            return False

        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()
            
            purchased_col = self._get_purchased_column(cursor)
            new_sync_hash = generate_sync_hash()

            query = f"""
                UPDATE groceries
                SET 
                    {purchased_col} = 1,
                    status = 'modified',
                    is_synced = 0,
                    sync_hash = ?
                WHERE uid = ?
            """
            cursor.execute(query, (new_sync_hash, uid))
            conn.commit()
            success = cursor.rowcount > 0
            conn.close()
            return success
        except Exception as e:
            log.error(f"Failed to mark grocery as purchased: {e}")
            return False
