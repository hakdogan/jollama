# Jollama

`Ollama` is a lightweight, extensible framework for building, running, and chatting with large language models (LLMs) locally on your machine. This project demonstrates how to integrate with Ollama's API, manage model configurations via `Modelfiles`, and maintain conversation context across chat sessions.


## Features

- **Ollama API Integration:** Communicate seamlessly with your local Ollama server

- **Modelfile Support:** Load and manage custom model configurations

- **Context Awareness:** Maintain conversation context across requests for coherent multi-turn dialogues

- **Interactive Console Chat:** Simple console interface for testing and demonstration

- **Standalone Java Application:** No external dependencies beyond the Ollama server

## Prerequisites

- Java 17 or higher
- Ollama installed on your system
- At least one model pulled via Ollama _(e.g., ollama pull mistral)_

## Installation

Clone this repository:

```bash
git clone https://github.com/hakdogan/jollama
cd jollama
```

Create an executable Jar file:

```bash
javac -d mods --source-path "./src/*" \                                                                                     ✔  22  
$(find . -name "*.java")

cd mods/

jar cvmf ../manifest.txt jollama.jar .  
```

or

```bash
sh compile.sh
```


## Usage

```bash
java -jar jollama.jar
```

or

```bash
sh run.sh
```

To list local models, you can type **models** after the following message is displayed in the console

```bash
Before you begin, please specify the model you want to use.
(Type "models" to list local models)

models

gpt-oss
qwen2.5
phi3.5
phi3
all-minilm
qwen3
nomic-embed-text
llava
qwen2.5
llama3.2
qwen2.5
mistral

Please specify the model you want to use.
```
If you want to configure a model, you must provide the **model name** and **model file path**

```bash
java -jar jollama.jar java-expert src/main/resources/Modelfile

gathering model components 
using existing layer sha256:ff82381e2bea77d91c1b824c7afb83f6fb73e9f7de9dda631bcdbca564aa5435 
using existing layer sha256:43070e2d4e532684de521b885f385d0841030efa2b1a20bafb76133a5e1379c1 
using existing layer sha256:491dfa501e59ed17239711477601bdc7f559de5407fbd4a2a79078b271045621 
using existing layer sha256:69525e015f87b01e2746a48e53866575cc5f0a371d3dbce438be557a83713b0c 
using existing layer sha256:85b5c07d0bb2f5fff04c0d8fdaaeb698686fcd155220c9ce7a65913111b043ee 
writing manifest 
success 

Model created successfully: java-expert
Before you begin, please specify the model you want to use.
(Type "models" to list local models)

java-expert

Ask anything. Type exit to exit!
What is Java?

Java is a programming language. It was designed by Sun Microsystems in 1995...
```