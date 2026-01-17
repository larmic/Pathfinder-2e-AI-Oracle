#!/bin/bash

# Start Ollama server in background
ollama serve &

# Wait for Ollama to be ready
echo "Waiting for Ollama to start..."
while ! ollama list > /dev/null 2>&1; do
    sleep 1
done
echo "Ollama is ready!"

# Pull required models if not already present
if ! ollama list | grep -q "nomic-embed-text"; then
    echo "Pulling nomic-embed-text model..."
    ollama pull nomic-embed-text
    echo "nomic-embed-text model ready!"
else
    echo "nomic-embed-text model already available"
fi

if ! ollama list | grep -q "qwen2.5:7b"; then
    echo "Pulling qwen2.5:7b model..."
    ollama pull qwen2.5:7b
    echo "qwen2.5:7b model ready!"
else
    echo "qwen2.5:7b model already available"
fi

# Keep container running
wait
