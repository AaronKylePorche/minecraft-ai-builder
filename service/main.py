"""
BuilderGPT Microservice
FastAPI bridge for Minecraft BuilderBot plugin to generate structures using BuilderGPT + OpenAI GPT.

Endpoint: POST /generate
- Accepts structure description, minecraft version, output format
- Calls BuilderGPT programmatically
- Returns .schem file path and metadata
"""

import os
import sys
import time
import json
from pathlib import Path
from typing import Optional
from datetime import datetime

from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
from pydantic import BaseModel
import uvicorn

# Import BuilderGPT component (bundled in components/BuilderGPT; CyniaAgents comes from requirements.txt)
try:
    from components.BuilderGPT.component import BuilderGPTComponent
    import config  # Load .env configuration
    print(f"[OK] CyniaAgents loaded successfully")

    # Check if API key is properly configured
    if hasattr(config, 'API_KEY') and config.API_KEY and config.API_KEY != "YOUR_API_KEY_HERE" and len(config.API_KEY) > 20:
        print(f"[OK] API_KEY configured: {config.API_KEY[:20]}...")
    else:
        print("[ERROR] API_KEY not configured!")
        print("  Please set API_KEY in the .env file next to Start.bat")
        sys.exit(1)
except ImportError as e:
    print(f"[ERROR] Failed to import CyniaAgents: {e}")
    print("  Make sure requirements.txt is installed (Start.bat does this on first run)")
    sys.exit(1)

# Constants
SHARED_OUTPUT_DIR = Path(__file__).resolve().parent / "generated"
GENERATION_TIMEOUT = 120  # seconds
DEFAULT_VERSION = "JE_1_21_1"

# Initialize FastAPI app
app = FastAPI(
    title="BuilderGPT Microservice",
    description="HTTP bridge for AI-powered Minecraft structure generation",
    version="1.0.0"
)

# Initialize BuilderGPT component
buildergpt = None

@app.on_event("startup")
async def startup_event():
    """Initialize BuilderGPT component on startup."""
    global buildergpt
    try:
        buildergpt = BuilderGPTComponent()
        print("[OK] BuilderGPT component initialized")
    except Exception as e:
        print(f"[ERROR] Failed to initialize BuilderGPT: {e}")
        raise


class GenerateRequest(BaseModel):
    """Request model for structure generation."""
    prompt: str
    minecraft_version: str = DEFAULT_VERSION
    output_format: str = "schem"  # "schem" or "mcfunction"


class GenerateResponse(BaseModel):
    """Response model for successful generation."""
    status: str
    schem_path: str
    metadata: dict


class ErrorResponse(BaseModel):
    """Response model for errors."""
    status: str
    message: str


@app.get("/")
async def root():
    """Health check endpoint."""
    return {
        "service": "BuilderGPT Microservice",
        "status": "running",
        "version": "1.0.0",
        "buildergpt_ready": buildergpt is not None
    }


@app.post("/generate", response_model=GenerateResponse, responses={500: {"model": ErrorResponse}})
async def generate_structure(request: GenerateRequest):
    """
    Generate a Minecraft structure using BuilderGPT.

    Args:
        request: GenerateRequest containing prompt, version, and format

    Returns:
        GenerateResponse with .schem path and metadata

    Raises:
        HTTPException: If generation fails
    """
    if buildergpt is None:
        raise HTTPException(
            status_code=500,
            detail="BuilderGPT component not initialized"
        )

    # Validate output format
    if request.output_format not in ["schem", "mcfunction"]:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid output_format: {request.output_format}. Must be 'schem' or 'mcfunction'"
        )

    print(f"\n🔨 Generating structure:")
    print(f"  Prompt: {request.prompt}")
    print(f"  Version: {request.minecraft_version}")
    print(f"  Format: {request.output_format}")

    start_time = time.time()

    try:
        # Call BuilderGPT synchronously (blocking)
        # BuilderGPT's generate() method handles the entire pipeline:
        # 1. LLM call to generate JS code
        # 2. Execute JS to build block placement list
        # 3. Convert to .schem format
        # 4. Save to service/generated/
        path = buildergpt.generate(
            description=request.prompt,
            version=request.minecraft_version,
            export_type=request.output_format,
            image_path=None,
            progress=None  # No Streamlit progress bar in API mode
        )

        if path is None or not os.path.exists(path):
            raise HTTPException(
                status_code=500,
                detail="BuilderGPT returned no output or file not found"
            )

        generation_time = time.time() - start_time
        file_size = os.path.getsize(path)

        # Extract approximate block count from file (rough estimate)
        # For .schem files, size correlates with complexity
        approx_block_count = max(100, file_size // 10)  # Very rough estimate

        print(f"[OK] Generation complete in {generation_time:.1f}s")
        print(f"  File: {path}")
        print(f"  Size: {file_size} bytes")

        return GenerateResponse(
            status="ok",
            schem_path=path,
            metadata={
                "approx_block_count": approx_block_count,
                "file_size_bytes": file_size,
                "generation_time_seconds": round(generation_time, 2),
                "timestamp": datetime.now().isoformat(),
                "minecraft_version": request.minecraft_version,
                "output_format": request.output_format
            }
        )

    except HTTPException:
        raise  # Re-raise HTTP exceptions
    except Exception as e:
        print(f"[ERROR] Generation failed: {e}")
        raise HTTPException(
            status_code=500,
            detail=f"LLM generation failed: {str(e)}"
        )


@app.get("/health")
async def health_check():
    """Detailed health check with configuration status."""
    api_key_configured = False
    try:
        api_key_configured = (
            hasattr(config, 'API_KEY') and
            config.API_KEY != "YOUR_API_KEY_HERE" and
            len(config.API_KEY) > 20
        )
    except:
        pass

    return {
        "status": "healthy" if (buildergpt is not None and api_key_configured) else "degraded",
        "buildergpt_initialized": buildergpt is not None,
        "api_key_configured": api_key_configured,
        "shared_output_dir": str(SHARED_OUTPUT_DIR),
        "output_dir_exists": SHARED_OUTPUT_DIR.exists(),
    }


if __name__ == "__main__":
    print("\n" + "="*60)
    print("BuilderGPT Microservice Starting...")
    print("="*60)
    print(f"Shared output directory: {SHARED_OUTPUT_DIR}")
    print(f"Default Minecraft version: {DEFAULT_VERSION}")
    print(f"Generation timeout: {GENERATION_TIMEOUT}s")
    print("="*60 + "\n")

    # Run with Uvicorn
    uvicorn.run(
        app,
        host="127.0.0.1",
        port=8001,
        log_level="info"
    )
