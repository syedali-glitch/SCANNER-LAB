---
description: Push changes to git and trigger a build via GitHub Actions workflow
---

This workflow ensures all pending changes are committed and pushed to the remote repository, which will automatically trigger the `Android CI/CD` GitHub Actions workflow.

1. Ensure all files are tracked and staged
// turbo
2. git add .

3. Commit changes with a descriptive message
// turbo
4. git commit -m "Build [$(date +'%Y-%m-%d %H:%M:%S')]: Comprehensive update and build trigger"

5. Push to the main branch
// turbo
6. git push origin main

7. Verify that the push was successful. You should see a new run starting in the Actions tab of the GitHub repository.
