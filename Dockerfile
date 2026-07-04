FROM python:3.12-slim

WORKDIR /app

# Install uv for fast dependency resolution
RUN pip install uv

# Copy project files
COPY pyproject.toml .
COPY README.md .
COPY LICENSE.md .
COPY kappari/ kappari/

# Install the kappari package and dependencies
RUN uv pip install --system -e .

# Set the entrypoint to the sync daemon
# Environment variables should be passed to the container
# e.g. docker run -e KAPPARI_KEEP_EMAIL=... -e KAPPARI_KEEP_APP_PASSWORD=... kappari-sync
ENTRYPOINT ["python", "-m", "kappari.sync_daemon"]
