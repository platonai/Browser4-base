# Security Policy

## Supported Versions

We release patches for security vulnerabilities in the following versions:

| Version | Supported          |
| ------- | ------------------ |
| 0.1.x   | :white_check_mark: |

As new versions are released, we will update this table accordingly.

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues.**

Instead, please report them responsibly by sending an email to:

**security@platonai.com** (or the appropriate security contact email)

Please include the following information in your report:

- **Description**: A clear description of the vulnerability
- **Steps to Reproduce**: Detailed steps to reproduce the issue
- **Impact**: Potential impact and attack scenarios
- **Affected Versions**: Which versions of browser4-python are affected
- **Suggested Fix**: If you have a suggested fix or mitigation (optional)
- **Your Contact**: How we can reach you for follow-up questions

### What to Expect

1. **Acknowledgment**: You should receive an acknowledgment of your report within 48 hours
2. **Assessment**: We will assess the vulnerability and its impact
3. **Fix Development**: If confirmed, we will work on a fix
4. **Disclosure**: We will coordinate the disclosure timeline with you
5. **Credit**: You will be credited in the security advisory (if desired)

### Security Update Process

When a security vulnerability is confirmed:

1. We will develop and test a fix in a private repository
2. We will prepare security advisories
3. We will release patched versions for all supported releases
4. We will publish a security advisory on GitHub
5. We will notify users through appropriate channels

### Security Best Practices for Users

When using browser4-python:

1. **Keep Updated**: Always use the latest version of the SDK
2. **Validate Input**: Validate and sanitize all user inputs before passing to the SDK
3. **Secure Credentials**: Never hardcode API keys or credentials
4. **Network Security**: Use HTTPS for all communications with the Browser4 server
5. **Dependency Management**: Regularly update dependencies and scan for vulnerabilities
6. **Principle of Least Privilege**: Run the SDK with minimal required permissions

### Known Security Considerations

1. **JavaScript Execution**: The `execute_script` method allows arbitrary JavaScript execution. Only use trusted scripts.
2. **Remote Server**: The SDK communicates with a Browser4 server. Ensure the server is trusted and secured.
3. **File Downloads**: Browser4Driver downloads Browser4.jar from GitHub. Verify the download source.
4. **Proxy Configuration**: If using a proxy, ensure it is trusted and secure.

## Vulnerability Disclosure Policy

We follow the principle of **Responsible Disclosure**:

- We request that security researchers allow us reasonable time to fix vulnerabilities before public disclosure
- We commit to working with researchers to understand and fix reported vulnerabilities
- We will publicly acknowledge researchers who report vulnerabilities (unless they prefer to remain anonymous)
- We will not take legal action against researchers who follow this policy

## Security Updates

Security updates will be released as:
- **Patch versions** (e.g., 0.1.0 → 0.1.1) for minor vulnerabilities
- **Minor versions** (e.g., 0.1.x → 0.2.0) for moderate vulnerabilities
- **Major versions** (e.g., 0.x → 1.0) for critical vulnerabilities requiring breaking changes

All security updates will be documented in:
- CHANGELOG.md
- GitHub Security Advisories
- GitHub Releases

## Contact

- **Security Email**: security@platonai.com
- **GitHub Security**: https://github.com/platonai/Browser4/security
- **General Contact**: https://github.com/platonai/Browser4/issues (for non-security issues only)

Thank you for helping keep browser4-python and its users secure!
