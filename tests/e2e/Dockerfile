FROM eclipse-temurin:25-jdk-noble

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ca-certificates curl git jq tini procps \
        xvfb xauth x11-utils xdotool xclip openbox imagemagick \
        libgl1-mesa-dri libglx-mesa0 mesa-utils libasound2t64 \
        libxtst6 libxi6 libxrandr2 libxcursor1 libxinerama1 libxxf86vm1 \
    && rm -rf /var/lib/apt/lists/*

ENV DISPLAY=:99 \
    LIBGL_ALWAYS_SOFTWARE=1 \
    GRADLE_USER_HOME=/cache/gradle \
    E2E_ARTIFACTS=/artifacts \
    E2E_CONTROL=/tmp/e2e-control

COPY tests/e2e/runtime/ /opt/e2e/
COPY . /workspace/
WORKDIR /workspace

ENTRYPOINT ["/usr/bin/tini", "-g", "--", "bash", "/opt/e2e/session.sh"]
CMD ["bash", "tests/e2e/probes/run.sh"]
