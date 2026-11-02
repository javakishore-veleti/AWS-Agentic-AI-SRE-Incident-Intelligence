from fastapi.testclient import TestClient

from app.main import app


def test_desk_origin_can_call_the_agent():
    response = TestClient(app).options(
        "/api/investigations",
        headers={
            "Origin": "http://127.0.0.1:5173",
            "Access-Control-Request-Method": "POST",
            "Access-Control-Request-Headers": "content-type",
        },
    )
    assert response.headers["access-control-allow-origin"] == "http://127.0.0.1:5173"
