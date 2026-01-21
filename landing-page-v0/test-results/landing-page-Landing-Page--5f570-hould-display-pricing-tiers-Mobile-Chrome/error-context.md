# Page snapshot

```yaml
- generic [active] [ref=e1]:
  - link "Skip to main content" [ref=e2] [cursor=pointer]:
    - /url: "#main-content"
  - alert [ref=e3]
  - status [ref=e4]
  - alert [ref=e5]
  - navigation "Global navigation" [ref=e6]:
    - generic [ref=e8]:
      - link "🏗️ Bolduc Builders" [ref=e10] [cursor=pointer]:
        - /url: /
        - generic [ref=e11]: 🏗️
        - generic [ref=e12]: Bolduc Builders
      - generic [ref=e13]:
        - button "Open menu" [ref=e14] [cursor=pointer]:
          - img [ref=e15]
        - button "Search (Cmd+K)" [ref=e17] [cursor=pointer]:
          - img [ref=e18]
        - button "Toggle theme (currently light)" [ref=e20] [cursor=pointer]:
          - img [ref=e21]
          - generic [ref=e27]: Switch to dark theme
        - link "Notifications" [ref=e28] [cursor=pointer]:
          - /url: /notifications
          - img [ref=e29]
        - link "Login" [ref=e32] [cursor=pointer]:
          - /url: /login
  - main [ref=e33]:
    - generic [ref=e35]:
      - generic [ref=e36]: "404"
      - heading "Page Not Found" [level=1] [ref=e37]
      - paragraph [ref=e38]: The page you're looking for doesn't exist or has been moved.
      - generic [ref=e39]:
        - link "Go Home" [ref=e40] [cursor=pointer]:
          - /url: /
        - link "Dashboard" [ref=e41] [cursor=pointer]:
          - /url: /dashboard
      - paragraph [ref=e42]: If you believe this is a mistake, please contact support.
```