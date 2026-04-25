import os
import shutil
import git
from tree_sitter_languages import get_language, get_parser
from sentence_transformers import SentenceTransformer
import chromadb

# Load embedding model once at startup
embedding_model = SentenceTransformer("microsoft/codebert-base")

# Supported file extensions and their tree-sitter languages
LANGUAGE_MAP = {
    ".py": "python",
    ".java": "java",
    ".js": "javascript",
    ".ts": "typescript",
    ".go": "go",
    ".rb": "ruby",
    ".cpp": "cpp",
    ".c": "c",
}

def clone_repo(github_url: str, repo_id: str, repos_path: str) -> str:
    repo_dir = os.path.join(repos_path, repo_id)
    if os.path.exists(repo_dir):
        shutil.rmtree(repo_dir)
    print(f"Cloning {github_url} into {repo_dir}")
    git.Repo.clone_from(github_url, repo_dir)
    return repo_dir

def get_code_files(repo_dir: str) -> list:
    code_files = []
    for root, dirs, files in os.walk(repo_dir):
        # Skip hidden and dependency folders
        dirs[:] = [d for d in dirs if d not in
                   {'.git', 'node_modules', '__pycache__', '.venv', 'venv', 'target', 'build'}]
        for file in files:
            ext = os.path.splitext(file)[1]
            if ext in LANGUAGE_MAP:
                code_files.append(os.path.join(root, file))
    return code_files

def chunk_file(file_path: str) -> list:
    ext = os.path.splitext(file_path)[1]
    language_name = LANGUAGE_MAP.get(ext)

    with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
        source_code = f.read()

    if not source_code.strip():
        return []

    chunks = []

    if language_name:
        try:
            language = get_language(language_name)
            parser = get_parser(language_name)
            tree = parser.parse(bytes(source_code, "utf-8"))

            # Extract functions and classes as chunks
            query_patterns = {
                "python": "(function_definition) @func (class_definition) @class",
                "java": "(method_declaration) @method (class_declaration) @class",
                "javascript": "(function_declaration) @func (arrow_function) @arrow (class_declaration) @class",
                "typescript": "(function_declaration) @func (class_declaration) @class",
            }

            pattern = query_patterns.get(language_name)
            if pattern:
                query = language.query(pattern)
                captures = query.captures(tree.root_node)
                for node, _ in captures:
                    chunk_text = source_code[node.start_byte:node.end_byte]
                    if len(chunk_text.strip()) > 50:
                        chunks.append({
                            "text": chunk_text,
                            "file": file_path,
                            "start_line": node.start_point[0],
                            "end_line": node.end_point[0],
                            "type": "ast_chunk"
                        })
        except Exception as e:
            print(f"AST parsing failed for {file_path}: {e}")

    # Fallback: if no AST chunks, use sliding window
    if not chunks:
        lines = source_code.split("\n")
        window_size = 50
        step = 25
        for i in range(0, len(lines), step):
            window = "\n".join(lines[i:i + window_size])
            if window.strip():
                chunks.append({
                    "text": window,
                    "file": file_path,
                    "start_line": i,
                    "end_line": min(i + window_size, len(lines)),
                    "type": "window_chunk"
                })

    return chunks

def ingest_repo(event: dict, chroma_path: str, repos_path: str):
    repo_id = event["repoId"]
    github_url = event["githubUrl"]
    github_token = event.get("githubToken")

    # Build authenticated URL if token provided
    if github_token:
        url_parts = github_url.replace("https://", "")
        clone_url = f"https://{github_token}@{url_parts}"
    else:
        clone_url = github_url

    # Clone the repo
    repo_dir = clone_repo(clone_url, repo_id, repos_path)

    # Get all code files
    code_files = get_code_files(repo_dir)
    print(f"Found {len(code_files)} code files")

    # Chunk all files
    all_chunks = []
    for file_path in code_files:
        chunks = chunk_file(file_path)
        all_chunks.extend(chunks)

    print(f"Generated {len(all_chunks)} chunks")

    if not all_chunks:
        print("No chunks generated — skipping embedding")
        return

    # Generate embeddings
    texts = [chunk["text"] for chunk in all_chunks]
    print("Generating embeddings...")
    embeddings = embedding_model.encode(texts, show_progress_bar=True)

    # Store in ChromaDB
    client = chromadb.PersistentClient(path=chroma_path)
    collection = client.get_or_create_collection(
        name=f"repo_{repo_id}",
        metadata={"repo_id": repo_id, "github_url": github_url}
    )

    # Add in batches of 100
    batch_size = 100
    for i in range(0, len(all_chunks), batch_size):
        batch_chunks = all_chunks[i:i + batch_size]
        batch_embeddings = embeddings[i:i + batch_size].tolist()
        batch_ids = [f"{repo_id}_{i + j}" for j in range(len(batch_chunks))]
        batch_docs = [chunk["text"] for chunk in batch_chunks]
        batch_meta = [{"file": chunk["file"], "start_line": chunk["start_line"],
                       "type": chunk["type"]} for chunk in batch_chunks]

        collection.add(
            ids=batch_ids,
            embeddings=batch_embeddings,
            documents=batch_docs,
            metadatas=batch_meta
        )

    print(f"Stored {len(all_chunks)} chunks in ChromaDB for repo {repo_id}")