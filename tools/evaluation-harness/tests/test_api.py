"""Tests for the evaluation API."""

import pytest
from fastapi.testclient import TestClient

from evaluation_harness.api import app


@pytest.fixture
def client():
    """Create test client."""
    return TestClient(app)


class TestHealthEndpoint:
    """Tests for health check endpoint."""

    def test_health_check(self, client):
        """Health endpoint should return UP status."""
        response = client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "UP"
        assert "version" in data
        assert "metrics_available" in data


class TestEvaluateEndpoint:
    """Tests for single metric evaluation endpoint."""

    def test_evaluate_relevancy(self, client):
        """Should evaluate relevancy metric."""
        response = client.post(
            "/evaluate",
            json={
                "metricType": "RELEVANCY",
                "userMessage": "What are the care gaps for this patient?",
                "agentResponse": "The patient has outstanding care gaps for HbA1c testing.",
                "contextData": {},
            },
        )
        assert response.status_code == 200
        data = response.json()
        assert data["metricType"] == "RELEVANCY"
        assert 0.0 <= data["score"] <= 1.0
        assert "reason" in data

    def test_evaluate_hipaa_compliance(self, client):
        """Should evaluate HIPAA compliance metric."""
        response = client.post(
            "/evaluate",
            json={
                "metricType": "HIPAA_COMPLIANCE",
                "userMessage": "What is the patient status?",
                "agentResponse": "The patient has diabetes and needs follow-up care.",
                "contextData": {},
            },
        )
        assert response.status_code == 200
        data = response.json()
        assert data["metricType"] == "HIPAA_COMPLIANCE"
        assert data["score"] >= 0.9  # Clean response should pass

    def test_evaluate_unknown_metric(self, client):
        """Should return error for unknown metric type."""
        response = client.post(
            "/evaluate",
            json={
                "metricType": "UNKNOWN_METRIC",
                "userMessage": "test",
                "agentResponse": "test",
                "contextData": {},
            },
        )
        assert response.status_code == 422  # Validation error


class TestBatchEvaluateEndpoint:
    """Tests for batch evaluation endpoint."""

    def test_batch_evaluate(self, client):
        """Should evaluate multiple metrics at once."""
        response = client.post(
            "/evaluate/batch",
            json={
                "userMessage": "What medication should I take?",
                "agentResponse": "Please consult your healthcare provider about medication options.",
                "contextData": {},
                "metricTypes": ["RELEVANCY", "CLINICAL_SAFETY", "HIPAA_COMPLIANCE"],
            },
        )
        assert response.status_code == 200
        data = response.json()
        assert len(data["results"]) == 3
        assert "overallScore" in data
        assert 0.0 <= data["overallScore"] <= 1.0


class TestMetricsEndpoint:
    """Tests for metrics listing endpoint."""

    def test_list_available_metrics(self, client):
        """Should list all available metrics."""
        response = client.get("/metrics/available")
        assert response.status_code == 200
        data = response.json()
        assert "metrics" in data
        assert len(data["metrics"]) > 0

        # Check that healthcare metrics are included
        metric_types = [m["type"] for m in data["metrics"]]
        assert "HIPAA_COMPLIANCE" in metric_types
        assert "CLINICAL_SAFETY" in metric_types
        assert "RELEVANCY" in metric_types
