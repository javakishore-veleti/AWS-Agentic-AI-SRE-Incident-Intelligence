import httpx

SERVICES = {
    "product-catalog-service": "http://localhost:8081/actuator/health",
    "offers-service": "http://localhost:8082/actuator/health",
    "customers-service": "http://localhost:8083/actuator/health",
    "orders-service": "http://localhost:8084/actuator/health",
    "inventory-service": "http://localhost:8085/actuator/health",
}


def collect_signals() -> list[dict]:
    signals = []
    with httpx.Client(timeout=2.0) as client:
        for name, url in SERVICES.items():
            try:
                response = client.get(url)
                body = response.json() if response.headers.get("content-type", "").startswith("application/json") else {}
                signals.append({
                    "service": name,
                    "url": url,
                    "httpStatus": response.status_code,
                    "reportedStatus": body.get("status", "UNKNOWN"),
                })
            except httpx.HTTPError as exc:
                signals.append({
                    "service": name,
                    "url": url,
                    "httpStatus": None,
                    "reportedStatus": "UNREACHABLE",
                    "error": str(exc),
                })
    return signals
