import time
from . import log
from .config import get_config
from .db_groceries import GroceriesDB
from .keep_sync import KeepSync

def main():
    config = get_config()

    if not config.keep_email or not config.keep_app_password:
        log.error("Google Keep credentials are not configured. Please set KAPPARI_KEEP_EMAIL and KAPPARI_KEEP_APP_PASSWORD.")
        return

    db_client = GroceriesDB(config.db_file)
    keep_sync = KeepSync(config.keep_email, config.keep_app_password)
    
    interval_seconds = config.sync_daemon_interval * 60
    
    log.info(f"Starting Keep Sync Daemon. Interval: {config.sync_daemon_interval} minutes.")
    
    while True:
        try:
            log.info("Starting synchronization cycle...")
            
            # 1. Fetch groceries from Paprika DB
            groceries = db_client.get_groceries()
            
            if groceries is not None:
                # 2. Sync with Keep
                keep_sync.sync_lists(config.keep_list_titles, groceries, db_client)
            else:
                log.warning("Could not fetch groceries from database, skipping this cycle.")
                
            log.info(f"Synchronization cycle completed. Sleeping for {interval_seconds} seconds.")
        except Exception as e:
            log.error(f"Error during synchronization cycle: {e}")
            
        time.sleep(interval_seconds)

if __name__ == "__main__":
    main()
