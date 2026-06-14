# AI model assets (not committed to git)

Place Play Asset Delivery files here before building a release AAB:

| File | Size | Source |
|------|------|--------|
| `gemma3-1b-it-int4.task` | ~555 MB | [Kaggle Gemma 3](https://www.kaggle.com/models/google/gemma-3/tfLite) |
| `Gecko_256_quant.tflite` | ~109 MB | Download script below |
| `sentencepiece.model` | ~776 KB | Download script below |

Download embedder + tokenizer:

```bash
./scripts/download_rag_models.sh
```

These files are gitignored because they exceed GitHub's 100 MB limit. They are delivered to users via Google Play Asset Delivery (`install-time` fast-follow pack).
