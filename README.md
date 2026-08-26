# discovery — Android fork (`26.4.0-android`)

> **This branch is a minimal Android-compatibility fork of
> [ConsenSys/discovery](https://github.com/Consensys/discovery) `26.4.0`,
> consumed by [biafra23/myotis](https://github.com/biafra23/myotis) via JitPack
> as `com.github.biafra23:discovery:<tag>` (tags: `26.4.0-android.N`).**
>
> Upstream 26.4.0 calls six JDK methods that do not exist on Android below API
> 31/34/35 — myotis ships minSdk 29, and Android 10/11 never receive ART module
> backports; none of these are covered by D8 backports (and only `Stream.toList`
> is covered by desugar_jdk_libs, from 2.1.5):
>
> - `KBucket`: `List.addFirst`/`List.getLast` (SequencedCollection, API 35) and
>   `Stream.takeWhile` (API 34) — the core Kademlia routing table, so the first
>   liveness confirmation or the first offer to a full bucket (seconds after
>   startup) crashes.
> - `KBuckets`: `Stream.toList` (API 34) in `getNodeRecordBuckets()`.
> - `RecursiveLookupTasks` and `NettyDiscoveryServerImpl`:
>   `CompletableFuture.failedFuture` (API 31).
> - `DiscoverySystemBuilder.AsyncPinger`: `CompletableFuture.orTimeout`
>   (API 31).
>
> Each call is replaced with a semantically identical pre-API-29 form
> (`add(0, e)`, `get(size() - 1)`, an imperative prefix loop,
> `collect(Collectors.toList())`, a completed-exceptionally future, and a
> shared-daemon-timer `orTimeout` mirroring the JDK's internal Delayer). No
> behavior change on any JVM; the patched sites are marked with `Android fork:`
> comments.
>
> To rebase onto a newer upstream release: branch from the new upstream tag,
> re-apply the patches (`git log 26.4.0..26.4.0-android`, or grep for
> `Android fork:`), then verify against the myotis APK: dexdump the assembled
> APK and confirm zero references to the six members above (myotis's standing
> checker for this is `scripts/check_apk_min_api.py` once the minsdk-29 audit
> branch lands there). Then tag `<base>-android.1` and push — the same playbook
> as [biafra23/besu](https://github.com/biafra23/besu)
> (`besu-android-fork/README.md` in myotis).

---

[![ci](https://github.com/Consensys/discovery/actions/workflows/ci.yml/badge.svg)](https://github.com/Consensys/discovery/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.consensys.protocols/discovery)](https://central.sonatype.com/artifact/io.consensys.protocols/discovery)

## Overview

This is a Java implementation of the [Discovery v5](https://github.com/ethereum/devp2p/blob/master/discv5/discv5.md)
peer discovery protocol.

## Dependency

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.consensys.protocols:discovery:<version>")
}
```
