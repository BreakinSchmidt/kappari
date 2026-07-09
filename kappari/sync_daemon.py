import time
from . import log
from .config import get_config
from .network_client import get_client
from .todoist_sync import TodoistSync

def main():
    config = get_config()

    if not config.todoist_token:
        log.error("Todoist credentials are not configured. Please set KAPPARI_TODOIST_TOKEN in .env.")
        return
        
    if not config.email or not config.password:
        log.error("Paprika credentials are not configured. Please set KAPPARI_EMAIL and KAPPARI_PASSWORD in .env.")
        return

    paprika_client = get_client()
    jwt_token = paprika_client.authenticate(config.email, config.password)
    if not jwt_token:
        log.error("Failed to authenticate with Paprika API. Exiting.")
        return
        
    todoist_sync = TodoistSync(config.todoist_token)
    
    interval_seconds = config.sync_daemon_interval * 60
    
    log.info(f"Starting Todoist Sync Daemon. Interval: {config.sync_daemon_interval} minutes.")
    
    while True:
        try:
            log.info("Starting synchronization cycle...")
            
            # 1. Fetch groceries from Paprika API
            groceries = paprika_client.get_groceries(jwt_token)
            
            if groceries is not None:
                # 2. Sync with Todoist
                todoist_sync.sync_lists(config.todoist_project_name, groceries, paprika_client, jwt_token)
            else:
                log.warning("Could not fetch groceries from database, skipping this cycle.")
                
            log.info(f"Synchronization cycle completed. Sleeping for {interval_seconds} seconds.")
        except Exception as e:
            log.error(f"Error during synchronization cycle: {e}")
            
        time.sleep(interval_seconds)

if __name__ == "__main__":
    main()
