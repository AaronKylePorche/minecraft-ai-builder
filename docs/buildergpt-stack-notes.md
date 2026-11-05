# BuilderGPT AI Stack Debug Notes

✅ **WORKING CONFIGURATION** (Tested 2025-12-04)

## Answers
- ✅ **Startup command**: `python main.py` is CORRECT. The main.py file internally calls `uvicorn.run(app, host="127.0.0.1", port=8001)` at line 219-224.
- ✅ **Virtual environment**: `D:\ai\cynia\.venv312\Scripts\activate.bat` is CORRECT and working.
- ✅ **Working directory**: `D:\ai\buildergpt-service` is CORRECT.
- ✅ **Environment variables**: Only `D:\ai\cynia\.env` is needed with these keys:
  - `API_KEY` - OpenAI API key (required)
  - `LLM_PROVIDER=openai`
  - `GENERATION_MODEL=gpt-4.1-mini`
  - `FIXING_MODEL=gpt-4.1-mini`
  - `VERSION_NUMBER=0.0.1`

## Fixed Issues (2025-12-04)
1. ✅ **ENV loading**: Batch script now explicitly loads environment variables from D:\ai\cynia\.env before starting Python. This ensures variables are available regardless of working directory.
2. ✅ **Misleading warning**: Fixed main.py to properly detect API key configuration and exit with error if not found.

## Known Issues (Non-blocking)
1. **Unicode error**: First run may show `UnicodeEncodeError` for checkmark character due to Windows console encoding. Service still starts successfully on retry.
2. **Deprecation warning**: `@app.on_event("startup")` is deprecated. Should migrate to lifespan event handlers (not urgent, still works).

## Successful Startup Output
```
config: API_KEY -> ********
Initialized the openai LLM client with model gpt-4.1-mini.
[OK] BuilderGPT component initialized
INFO:     Uvicorn running on http://127.0.0.1:8001 (Press CTRL+C to quit)
```

## Health Check Verification
Run: `curl http://127.0.0.1:8001/health`

Expected response:
```json
{
  "status": "healthy",
  "buildergpt_initialized": true,
  "api_key_configured": true,
  "shared_output_dir": "D:\\ai\\mc-builds\\generated",
  "output_dir_exists": true
}
```

## Current Status
- ✅ AI service is running successfully on http://127.0.0.1:8001
- ✅ API key configured and loaded
- ✅ BuilderGPT initialized
- ✅ Health endpoints responding
- 🔜 Next: Build Minecraft plugin with Maven
