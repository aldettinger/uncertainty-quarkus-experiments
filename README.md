# uncertainty-quarkus-experiments

Find here the latest state of experiments related to the Granite uncertainty model.
During preliminary work, a lot of issues were uncovered, especially while trying the serve the uncertainty model.
At the end of the day, 2 resulting approaches were conclusive.

# Let's build local version of vLLM that can run without any GPU

To run on CPU, one needs to build vLLM locally, like below.

```
cd ~/dev/projects/vllm-upstream
git fetch
git checkout v0.8.5
docker build -f docker/Dockerfile.cpu -t vllm-0.8.5-cpu-env --shm-size=4g .
```

# Approach 1: Merge the base model and LoRa adapter on an external platform

Hitting so much issues to merge the base model and LoRa adapter on the local machine, I ended up merging them manually on [Kaggle](https://www.kaggle.com/code/alexdettinger/merging-uncertainty-lora).
To reproduce, run all the Kaggle steps and download the resulting zip file containing the merged model.
Unzip the Kaggle generated file to `dev/hugging-face-models/granite-3.2-8b-instruct-merged-with-uncertainty-lora/`.
From there, it should be possible to run the merged model that way:

```
docker run -it --mount type=bind,source=/home/agallice/dev/hugging-face-models/,target=/hf-models --rm --network=host vllm-0.8.5-cpu-env --model /hf-models/granite-3.2-8b-instruct-merged-with-uncertainty-lora --max-model-len 16384
```

Wait for the startup to complete:

```
INFO:     Started server process [1]
INFO:     Waiting for application startup.
INFO:     Application startup complete.

```

Now ask a question with the certainty role:

```
  curl -X POST "http://localhost:8000/v1/chat/completions" \
    -H "Content-Type: application/json" \
    --data '{
        "model": "/hf-models/granite-3.2-8b-instruct-merged-with-uncertainty-lora",
        "max_tokens": 3,
        "messages": [
            {
                "role": "certainty",
                "content": "What is the capital of France?"
            }
        ]
    }'
```

Hopefully, it will answer with a certainty percentage, like `"content":"85%"`.
On my machine, the certainty score for the capital of France question varies between 80% and 90%.
That would be an indication that the model is confident to answer this question correctly most of the time.

# Approach 2: Serve the base model and LoRa adapter locally

The bug [VLLM-17396](https://github.com/vllm-project/vllm/issues/17396) has been fixed in commit [f2c3f66](https://github.com/vllm-project/vllm/commit/f2c3f66d59f9e38aa94985b54f370219222e7bd1). So, with vllm > 0.9.1, it should be possible to serve the model without merging manually on an external platform.

```
docker run -it --privileged=true --mount type=bind,source=/home/agallice/dev/hugging-face-models/,target=/hf-models --rm --network=host vllm-0.9.1-cpu-env --model /hf-models/granite-3.2-8b-instruct --max-model-len 16384 --enable-lora --lora-modules uncertainty-lora=/hf-models/granite-uncertainty-3.2-8b-lora
```

Wait for the startup to complete:

```
INFO:     Started server process [1]
INFO:     Waiting for application startup.
INFO:     Application startup complete.

```

In a second shell, ask a question to the uncertainty model with the certainty role:

```
  curl -X POST "http://localhost:8000/v1/chat/completions" \
    -H "Content-Type: application/json" \
    --data '{
        "model": "uncertainty-lora",
        "messages": [
            {
                "role": "certainty",
                "max_tokens": 3,
                "content": "What is the capital of France?"
            }
        ]
    }'
```

However, the model simply answer the question and does not provide any confidence score.
So the behavior differs from the model merged manually ?
