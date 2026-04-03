"""
Python SDK for Browser4 AgenticSession and WebDriver-compatible API.

This SDK provides a Python interface to the Browser4 browser automation platform,
enabling web scraping, data extraction, and AI-powered browser interaction.

Key classes:
- Browser4Driver: Automatic download and lifecycle management of Browser4 server
- PulsarClient: Low-level HTTP client for API communication
- PulsarSession: Session management for page loading and extraction
- AgenticSession: AI-powered browser automation (extends PulsarSession)
- WebDriver: Browser control and element interaction

Quick start:
    >>> from browser4 import Browser4Driver, PulsarClient, AgenticSession
    >>>
    >>> # Start Browser4 server automatically
    >>> with Browser4Driver() as driver:
    ...     # Create client and session
    ...     client = PulsarClient(base_url=driver.base_url)
    ...     session_id = client.create_session()
    ...     session = AgenticSession(client)
    ...
    ...     # Navigate and interact
    ...     session.driver.navigate_to("https://example.com")
    ...     print(session.driver.get_current_url())
    ...
    ...     # Use AI-powered actions
    ...     result = session.run("scroll to the bottom of the page")
    ...     print(result.success)
    ...
    ...     # Clean up
    ...     session.close()

See the README.md for more detailed usage examples.
"""

from .client import PulsarClient
from .agentic_session import PulsarSession, AgenticSession
from .webdriver import WebDriver
from .driver import Browser4Driver
from .models import (
    # Core data models
    WebPage,
    NormURL,
    # PageSnapshot,  # removed: use WebPage instead
    ElementRef,
    FieldsExtraction,
    # Agent result models
    AgentRunResult,
    AgentActResult,
    AgentObservation,
    ObserveResult,
    ExtractionResult,
    ToolCallResult,
    ActionDescription,
    AgentState,
    AgentHistory,
    ChatResponse,
    # Event system (placeholder)
    PageEventHandlers,
)

__all__ = [
    # Client
    "PulsarClient",
    # Driver
    "Browser4Driver",
    # Sessions
    "PulsarSession",
    "AgenticSession",
    # WebDriver
    "WebDriver",
    # Core models
    "WebPage",
    "NormURL",
    # "PageSnapshot",  # removed: use WebPage instead
    "ElementRef",
    "FieldsExtraction",
    # Agent models
    "AgentRunResult",
    "AgentActResult",
    "AgentObservation",
    "ObserveResult",
    "ExtractionResult",
    "ToolCallResult",
    "ActionDescription",
    "AgentState",
    "AgentHistory",
    "ChatResponse",
    # Events
    "PageEventHandlers",
]

__version__ = "0.1.0"
