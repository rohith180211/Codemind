import json
import os
from confluent_kafka import Consumer, KafkaError
from dotenv import load_dotenv
from app.ingestion import ingest_repo

load_dotenv()

KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
KAFKA_TOPIC = os.getenv("KAFKA_TOPIC", "repo-ingestion")
KAFKA_GROUP_ID = os.getenv("KAFKA_GROUP_ID", "ai-service-group")
CHROMA_PATH = os.getenv("CHROMA_PATH", "./chroma_db")
REPOS_PATH = os.getenv("REPOS_PATH", "./cloned_repos")

def start_consumer():
    os.makedirs(REPOS_PATH, exist_ok=True)
    os.makedirs(CHROMA_PATH, exist_ok=True)

    print(f"Starting Kafka consumer on topic: {KAFKA_TOPIC}")

    consumer = Consumer({
        "bootstrap.servers": KAFKA_BOOTSTRAP_SERVERS,
        "group.id": KAFKA_GROUP_ID,
        "auto.offset.reset": "earliest",
        "enable.auto.commit": False
    })

    consumer.subscribe([KAFKA_TOPIC])

    try:
        while True:
            msg = consumer.poll(timeout=1.0)

            if msg is None:
                continue

            if msg.error():
                if msg.error().code() == KafkaError._PARTITION_EOF:
                    continue
                else:
                    print(f"Kafka error: {msg.error()}")
                    break

            event = json.loads(msg.value().decode("utf-8"))
            repo_id = event.get("repoId")
            print(f"Received ingestion event for repo: {repo_id}")

            try:
                ingest_repo(event, CHROMA_PATH, REPOS_PATH)
                consumer.commit(asynchronous=False)
                print(f"Successfully ingested repo: {repo_id}")
            except Exception as e:
                print(f"Failed to ingest repo {repo_id}: {e}")

    finally:
        consumer.close()