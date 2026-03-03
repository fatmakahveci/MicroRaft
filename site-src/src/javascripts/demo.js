(function () {
    function byId(id) {
        return document.getElementById(id);
    }

    function bySelector(selector, root) {
        return (root || document).querySelector(selector);
    }

    function bySelectorAll(selector, root) {
        return Array.prototype.slice.call((root || document).querySelectorAll(selector));
    }

    function text(node, value) {
        if (node) {
            node.textContent = value;
        }
    }

    function SiteLayout() {
        this.main = bySelector('.col-md-9[role="main"]');
        this.sidebar = bySelector(".col-md-3");
        this.tocList = bySelector("#toc-collapse ul");
        this.home = bySelector(".mr-home");
        this.blog = bySelector(".mr-blog-shell, .mr-blog-index");
        this.demo = byId("mr-demo");
        this.pathname = window.location.pathname;
        this.init();
    }

    SiteLayout.prototype.init = function () {
        var body = document.body;
        if (!body || !this.main) {
            return;
        }

        if (this.home) {
            body.classList.add("mr-layout-home");
        } else if (this.blog) {
            body.classList.add("mr-layout-blog");
        } else if (this.demo) {
            body.classList.add("mr-layout-demo");
        } else {
            body.classList.add("mr-layout-docs");
        }

        this.applyDocsVariant(body);
        this.cleanupDuplicateDocTitle();
        this.applyTocVisibility(body);
    };

    SiteLayout.prototype.cleanupDuplicateDocTitle = function () {
        if (!document.body.classList.contains("mr-layout-docs")) {
            return;
        }

        var shellTitle = bySelector(".mr-doc-shell .mr-doc-title");
        var primaryHeading = bySelector('.col-md-9[role="main"] > h1');
        var tocAnchors = this.tocList ? bySelectorAll("a", this.tocList) : [];
        var expectedTitle = shellTitle ? (shellTitle.textContent || "").trim() : "";
        var headingText = primaryHeading ? (primaryHeading.textContent || "").trim() : "";
        var normalizedHeading = headingText.replace(/^title:\s*/i, "").trim();

        if (primaryHeading && expectedTitle && normalizedHeading === expectedTitle) {
            primaryHeading.parentNode.removeChild(primaryHeading);
        }

        if (!tocAnchors.length || !expectedTitle) {
            return;
        }

        var firstTocLink = tocAnchors[0];
        var firstTocText = ((firstTocLink && firstTocLink.textContent) || "").trim();
        if (firstTocText.replace(/^title:\s*/i, "").trim() !== expectedTitle) {
            return;
        }

        var firstTocItem = firstTocLink.closest("li");
        if (firstTocItem) {
            firstTocItem.parentNode.removeChild(firstTocItem);
        }
    };

    SiteLayout.prototype.applyDocsVariant = function (body) {
        if (!body.classList.contains("mr-layout-docs")) {
            return;
        }

        var explicitLayoutRoot = bySelector("[data-mr-doc-layout]", this.main) || bySelector(".mr-doc-shell[data-mr-doc-layout]");
        var explicitLayout = explicitLayoutRoot ? explicitLayoutRoot.getAttribute("data-mr-doc-layout") : "";
        var topLevelBlocks = bySelectorAll(":scope > p, :scope > ul, :scope > ol, :scope > blockquote, :scope > pre, :scope > .highlight", this.main);
        var headings = bySelectorAll("h2, h3", this.main);
        var cardCount = bySelectorAll(".mr-doc-card", this.main).length;
        var gridCount = bySelectorAll(".mr-doc-grid, .mr-doc-band", this.main).length;
        var articleSignals = bySelectorAll(".mr-snippet-note, .mr-section-break, .gist", this.main).length;
        var proseLength = topLevelBlocks.reduce(function (total, node) {
            return total + ((node.textContent || "").trim().length || 0);
        }, 0);
        var likelyArticle =
            proseLength >= 3200 && (topLevelBlocks.length >= 8 || headings.length >= 7 || articleSignals >= 2);
        var likelyReference =
            !!bySelector(".mr-doc-shell", this.main) &&
            !likelyArticle &&
            (cardCount >= 4 || gridCount >= 2 || (cardCount >= 2 && proseLength < 3200));
        var layout = explicitLayout === "reference" || explicitLayout === "article" ? explicitLayout : likelyReference ? "reference" : "article";

        body.classList.add(layout === "reference" ? "mr-doc-layout-reference" : "mr-doc-layout-article");
    };

    SiteLayout.prototype.applyTocVisibility = function (body) {
        if (!body.classList.contains("mr-layout-docs")) {
            body.classList.add("mr-toc-hidden");
            return;
        }

        var tocItems = this.tocList ? bySelectorAll("li", this.tocList) : [];
        var headings = bySelectorAll("h2, h3", this.main);
        var proseBlocks = bySelectorAll("p, li, pre, blockquote", this.main);
        var contentLength = proseBlocks.reduce(function (total, node) {
            return total + (node.textContent || "").trim().length;
        }, 0);
        var articleLayout = body.classList.contains("mr-doc-layout-article");
        var shouldShow = articleLayout
            ? tocItems.length >= 6 && headings.length >= 7 && contentLength >= 3600
            : tocItems.length >= 4 && headings.length >= 5 && contentLength >= 2200;

        body.classList.toggle("mr-toc-visible", shouldShow);
        body.classList.toggle("mr-toc-hidden", !shouldShow);
    };

    function NavEnhancements() {
        this.body = document.body;
        this.pathname = window.location.pathname.replace(/index\.html$/, "");
        this.navbar = bySelector(".navbar");
        this.init();
    }

    NavEnhancements.prototype.init = function () {
        this.markActiveItems();
        this.decorateHeader();
        this.improveMobileMenu();
    };

    NavEnhancements.prototype.markActiveItems = function () {
        var links = bySelectorAll(".navbar-nav a");
        var matched = null;

        links.forEach(
            function (link) {
                var href = link.getAttribute("href") || "";
                if (!href || href.indexOf("http") === 0 || href === "#") {
                    return;
                }

                var normalized = href.replace(/index\.html$/, "");
                var isCurrent =
                    normalized === this.pathname ||
                    (normalized !== "/" && this.pathname.indexOf(normalized) === 0 && normalized.length > 1);

                if (isCurrent) {
                    matched = link;
                    link.classList.add("is-current");
                    var parentLi = link.closest("li");
                    if (parentLi) {
                        parentLi.classList.add("active");
                    }
                    var dropdown = link.closest(".dropdown");
                    if (dropdown) {
                        dropdown.classList.add("active");
                    }
                }
            }.bind(this)
        );

        if (!matched) {
            var brand = bySelector(".navbar-brand");
            if (brand && this.pathname === "/") {
                brand.classList.add("is-current");
            }
        }
    };

    NavEnhancements.prototype.decorateHeader = function () {
        if (!this.navbar) {
            return;
        }
        this.body.classList.add("mr-has-sticky-header");
        this.syncScrollState();
        window.addEventListener("scroll", this.syncScrollState.bind(this), { passive: true });
    };

    NavEnhancements.prototype.syncScrollState = function () {
        this.body.classList.toggle("mr-header-scrolled", window.scrollY > 8);
    };

    NavEnhancements.prototype.improveMobileMenu = function () {
        var toggle = bySelector(".navbar-toggler, .navbar-toggle");
        var targetSelector = toggle ? toggle.getAttribute("data-bs-target") : null;
        var targetId = toggle ? toggle.getAttribute("aria-controls") : null;
        var collapse = targetSelector ? bySelector(targetSelector) : null;
        if (!collapse && targetId) {
            collapse = byId(targetId);
        }
        if (!collapse) {
            collapse = bySelector(".navbar-collapse");
        }
        if (!toggle || !collapse) {
            return;
        }

        var syncState = function () {
            var expanded = toggle.getAttribute("aria-expanded") === "true";
            var isOpen = collapse.classList.contains("show")
                || collapse.classList.contains("in")
                || expanded
                || !toggle.classList.contains("collapsed");
            document.body.classList.toggle("mr-mobile-nav-open", isOpen && window.innerWidth <= 991);
        };

        toggle.addEventListener("click", function () {
            window.setTimeout(syncState, 30);
        });

        collapse.addEventListener("shown.bs.collapse", syncState);
        collapse.addEventListener("hidden.bs.collapse", syncState);
        window.addEventListener("resize", syncState, { passive: true });

        if (window.MutationObserver) {
            new MutationObserver(syncState).observe(collapse, {
                attributes: true,
                attributeFilter: ["class"],
            });
            new MutationObserver(syncState).observe(toggle, {
                attributes: true,
                attributeFilter: ["aria-expanded", "class"],
            });
        }

        bySelectorAll(".navbar-collapse a").forEach(function (link) {
            link.addEventListener("click", function () {
                document.body.classList.remove("mr-mobile-nav-open");
            });
        });

        syncState();
    };

    function ReadingEnhancements() {
        this.body = document.body;
        this.main = bySelector('.col-md-9[role="main"]');
        this.heroMeta = bySelector(".mr-blog-meta");
        this.isBlogArticle = this.body.classList.contains("mr-layout-blog") && !!this.heroMeta;
        this.progress = null;
        this.init();
    }

    ReadingEnhancements.prototype.init = function () {
        if (!this.main || !this.isBlogArticle) {
            return;
        }

        this.addReadingTime();
        this.decorateImages();
        this.enableScrollProgress();
    };

    ReadingEnhancements.prototype.addReadingTime = function () {
        var words = (this.main.textContent || "").trim().split(/\s+/).filter(Boolean).length;
        var minutes = Math.max(1, Math.round(words / 220));
        var badge = document.createElement("span");
        badge.className = "mr-reading-time";
        badge.textContent = minutes + " min read";
        this.heroMeta.appendChild(document.createTextNode(" "));
        this.heroMeta.appendChild(badge);
    };

    ReadingEnhancements.prototype.decorateImages = function () {
        bySelectorAll('.col-md-9[role="main"] > img').forEach(function (image) {
            if (image.closest("figure")) {
                return;
            }

            var captionText = image.getAttribute("alt");
            var figure = document.createElement("figure");
            figure.className = "mr-figure";
            image.parentNode.insertBefore(figure, image);
            figure.appendChild(image);

            if (captionText) {
                var caption = document.createElement("figcaption");
                caption.textContent = captionText;
                figure.appendChild(caption);
            }
        });
    };

    ReadingEnhancements.prototype.enableScrollProgress = function () {
        this.progress = document.createElement("div");
        this.progress.className = "mr-reading-progress";
        this.progress.innerHTML = '<span class="mr-reading-progress-bar"></span>';
        document.body.appendChild(this.progress);
        this.updateProgress();
        window.addEventListener("scroll", this.updateProgress.bind(this), { passive: true });
        window.addEventListener("resize", this.updateProgress.bind(this));
    };

    ReadingEnhancements.prototype.updateProgress = function () {
        if (!this.progress) {
            return;
        }

        var bar = this.progress.firstElementChild;
        var start = this.main.offsetTop;
        var end = start + this.main.offsetHeight - window.innerHeight;
        var ratio = end <= start ? 1 : (window.scrollY - start) / (end - start);
        var clamped = Math.max(0, Math.min(1, ratio));
        bar.style.transform = "scaleX(" + clamped + ")";
    };

    function Demo() {
        this.root = byId("mr-demo");
        if (!this.root) {
            return;
        }

        this.nodes = [
            { id: "client", name: "client", role: "Client", kind: "client", logIndex: null, isolated: false, xRatio: 0.5, yRatio: 0.78 },
            { id: "node-a", name: "node-a", role: "Follower", kind: "raft", logIndex: 0, isolated: false, xRatio: 0.2, yRatio: 0.34 },
            { id: "node-b", name: "node-b", role: "Leader", kind: "raft", logIndex: 1, isolated: false, xRatio: 0.5, yRatio: 0.14 },
            { id: "node-c", name: "node-c", role: "Follower", kind: "raft", logIndex: 0, isolated: false, xRatio: 0.8, yRatio: 0.34 },
        ];
        this.term = 1;
        this.commitIndex = 1;
        this.pendingValue = "set value=42";
        this.leaderId = "node-b";
        this.partitioned = false;
        this.lines = [];
        this.logItems = [];
        this.timelineItems = [];
        this.tourActive = false;
        this.tourAutoplay = false;
        this.tourIndex = -1;
        this.tourTimer = null;
        this.tourDelayMs = 3200;
        this.panelMode = "beginner";
        this.panelModeCopy = {
            beginner:
                "Beginner mode keeps the right side focused on explanation, tour, and presets. Switch to Deep dive for the raw trace.",
            deep:
                "Deep dive mode exposes timeline, protocol flow, raw events, and presets so you can inspect the cluster state change by change.",
        };
        this.tourSteps = [
            {
                title: "Step 1: Replicate a healthy command",
                body: "Start with the normal Raft write path. A healthy leader accepts the write, followers acknowledge it, and the entry commits once quorum agrees.",
                action: function () {
                    this.replicate();
                },
            },
            {
                title: "Step 2: Isolate one follower",
                body: "Now cut off node-c. The cluster should degrade, but it can still make progress because two raft nodes still form a majority.",
                action: function () {
                    this.partition();
                },
            },
            {
                title: "Step 3: Commit with quorum still intact",
                body: "Commit one more command while node-c is still missing. This is the clearest way to see that Raft needs a majority, not every follower.",
                action: function () {
                    this.pendingValue = "compare-and-set value=42->84";
                    this.commandSelect.value = this.pendingValue;
                    this.replicate();
                },
            },
            {
                title: "Step 4: Recover the lagging follower",
                body: "Bring node-c back. Recovery means the follower must catch up until its committed state matches the rest of the cluster.",
                action: function () {
                    this.recover();
                },
            },
            {
                title: "Step 5: Rotate leadership",
                body: "Finish with a leader change. This shows that the cluster can continue operating across terms after successful commits and recovery.",
                action: function () {
                    this.electNextLeader();
                },
            },
        ];

        this.mount();
        this.bind();
        this.seed();
        this.render();
    }

    Demo.prototype.mount = function () {
        this.cluster = byId("mr-cluster");
        this.svg = byId("mr-demo-links");
        this.termValue = byId("mr-demo-term");
        this.leaderValue = byId("mr-demo-leader");
        this.commitValue = byId("mr-demo-commit");
        this.logList = byId("mr-demo-log");
        this.timelineList = byId("mr-demo-timeline");
        this.flowList = byId("mr-demo-flow");
        this.commandView = byId("mr-demo-command-view");
        this.quorumView = byId("mr-demo-quorum");
        this.quorumBadge = byId("mr-demo-quorum-badge");
        this.outcomeView = byId("mr-demo-outcome");
        this.explainerTitle = byId("mr-demo-explainer-title");
        this.explainerBody = byId("mr-demo-explainer-body");
        this.particleLayer = byId("mr-demo-particles");
        this.commandSelect = byId("mr-demo-command");
        this.leaderButton = byId("mr-demo-elect");
        this.replicateButton = byId("mr-demo-replicate");
        this.partitionButton = byId("mr-demo-partition");
        this.recoverButton = byId("mr-demo-recover");
        this.resetButton = byId("mr-demo-reset");
        this.happyButton = byId("mr-demo-scenario-happy");
        this.partitionScenarioButton = byId("mr-demo-scenario-partition");
        this.recoveryScenarioButton = byId("mr-demo-scenario-recovery");
        this.tourTitle = byId("mr-demo-tour-title");
        this.tourStep = byId("mr-demo-tour-step");
        this.tourBody = byId("mr-demo-tour-body");
        this.tourNextHint = byId("mr-demo-tour-next-hint");
        this.tourSpeedSelect = byId("mr-demo-tour-speed");
        this.modeSummary = byId("mr-demo-mode-summary");
        this.modeButtons = bySelectorAll("[data-mr-demo-mode]", this.root);
        this.modePanels = bySelectorAll("[data-mr-demo-view]", this.root);
        this.happyPathButton = byId("mr-demo-happy-path");
        this.tourStartButton = byId("mr-demo-tour-start");
        this.tourAutoButton = byId("mr-demo-tour-auto");
        this.tourNextButton = byId("mr-demo-tour-next");
        this.tourStopButton = byId("mr-demo-tour-stop");

        for (var i = 0; i < this.nodes.length; i += 1) {
            var node = this.nodes[i];
            var card = document.createElement("div");
            card.className = "mr-node" + (node.kind === "client" ? " is-client" : "");
            card.id = node.id;
            card.innerHTML =
                '<span class="mr-node-title"></span>' +
                '<span class="mr-node-role"></span>' +
                '<div class="mr-node-log"></div>';
            this.cluster.appendChild(card);
        }

        this.createLink("node-a", "node-b");
        this.createLink("node-b", "node-c");
        this.createLink("node-a", "node-c");
        this.positionNodes();
    };

    Demo.prototype.positionNodes = function () {
        if (!this.cluster) {
            return;
        }

        var clusterWidth = this.cluster.clientWidth;
        var clusterHeight = this.cluster.clientHeight;
        var layout =
            clusterWidth < 520
                ? {
                      client: { xRatio: 0.5, yRatio: 0.82 },
                      "node-a": { xRatio: 0.18, yRatio: 0.43 },
                      "node-b": { xRatio: 0.5, yRatio: 0.16 },
                      "node-c": { xRatio: 0.82, yRatio: 0.43 },
                  }
                : clusterWidth < 760
                  ? {
                        client: { xRatio: 0.5, yRatio: 0.8 },
                        "node-a": { xRatio: 0.16, yRatio: 0.37 },
                        "node-b": { xRatio: 0.5, yRatio: 0.14 },
                        "node-c": { xRatio: 0.84, yRatio: 0.37 },
                    }
                  : {
                        client: { xRatio: 0.5, yRatio: 0.8 },
                        "node-a": { xRatio: 0.16, yRatio: 0.35 },
                        "node-b": { xRatio: 0.5, yRatio: 0.12 },
                        "node-c": { xRatio: 0.84, yRatio: 0.35 },
                    };

        for (var i = 0; i < this.nodes.length; i += 1) {
            var node = this.nodes[i];
            var card = byId(node.id);
            if (!card) {
                continue;
            }

            var cardWidth = card.offsetWidth || (node.kind === "client" ? 164 : 148);
            var cardHeight = card.offsetHeight || 150;
            var point = layout[node.id] || node;
            var left = clusterWidth * point.xRatio - cardWidth / 2;
            var top = clusterHeight * point.yRatio - cardHeight / 2;
            var minLeft = Math.max(12, clusterWidth * 0.03);
            var maxLeft = Math.max(minLeft, clusterWidth - cardWidth - minLeft);
            var minTop = Math.max(10, clusterHeight * 0.03);
            var maxTop = Math.max(minTop, clusterHeight - cardHeight - minTop);

            card.style.left = Math.min(maxLeft, Math.max(minLeft, left)) + "px";
            card.style.top = Math.min(maxTop, Math.max(minTop, top)) + "px";
        }
    };

    Demo.prototype.createLink = function (fromId, toId) {
        var line = document.createElementNS("http://www.w3.org/2000/svg", "line");
        line.setAttribute("class", "mr-link");
        line.setAttribute("data-from", fromId);
        line.setAttribute("data-to", toId);
        this.svg.appendChild(line);
        this.lines.push(line);
    };

    Demo.prototype.bind = function () {
        var self = this;
        this.leaderButton.addEventListener("click", function () {
            self.electNextLeader();
        });
        this.commandSelect.addEventListener("change", function () {
            self.pendingValue = self.commandSelect.value;
            self.pushLog("Input", "Next command updated to \"" + self.pendingValue + "\".");
            self.render();
        });
        this.replicateButton.addEventListener("click", function () {
            self.replicate();
        });
        this.partitionButton.addEventListener("click", function () {
            self.partition();
        });
        this.recoverButton.addEventListener("click", function () {
            self.recover();
        });
        this.resetButton.addEventListener("click", function () {
            self.reset();
        });
        this.happyButton.addEventListener("click", function () {
            self.runHappyPath();
        });
        var happyDeepButton = byId("mr-demo-scenario-happy-deep");
        if (happyDeepButton) {
            happyDeepButton.addEventListener("click", function () {
                self.runHappyPath();
            });
        }
        this.partitionScenarioButton.addEventListener("click", function () {
            self.runPartitionScenario();
        });
        var partitionDeepButton = byId("mr-demo-scenario-partition-deep");
        if (partitionDeepButton) {
            partitionDeepButton.addEventListener("click", function () {
                self.runPartitionScenario();
            });
        }
        this.recoveryScenarioButton.addEventListener("click", function () {
            self.runRecoveryScenario();
        });
        var recoveryDeepButton = byId("mr-demo-scenario-recovery-deep");
        if (recoveryDeepButton) {
            recoveryDeepButton.addEventListener("click", function () {
                self.runRecoveryScenario();
            });
        }
        if (this.happyPathButton) {
            this.happyPathButton.addEventListener("click", function () {
                self.runHappyPath();
            });
        }
        this.modeButtons.forEach(function (button) {
            button.addEventListener("click", function () {
                self.setPanelMode(button.getAttribute("data-mr-demo-mode") || "beginner");
            });
        });
        this.tourStartButton.addEventListener("click", function () {
            self.startTour();
        });
        this.tourAutoButton.addEventListener("click", function () {
            self.startAutoplayTour();
        });
        this.tourSpeedSelect.addEventListener("change", function () {
            self.tourDelayMs = Number(self.tourSpeedSelect.value) || 3200;
            if (self.tourAutoplay) {
                self.scheduleAutoplayStep();
            }
        });
        this.tourNextButton.addEventListener("click", function () {
            self.advanceTour();
        });
        this.tourStopButton.addEventListener("click", function () {
            self.stopTour();
        });
        window.addEventListener("resize", function () {
            self.positionNodes();
            self.renderLinks();
        });
        this.setPanelMode(this.panelMode);
    };

    Demo.prototype.setPanelMode = function (mode) {
        this.panelMode = mode === "deep" ? "deep" : "beginner";
        if (this.root) {
            this.root.setAttribute("data-mr-demo-mode", this.panelMode);
        }
        this.modeButtons.forEach(
            function (button) {
                var isActive = button.getAttribute("data-mr-demo-mode") === this.panelMode;
                button.classList.toggle("is-active", isActive);
                button.setAttribute("aria-selected", isActive ? "true" : "false");
            }.bind(this)
        );
        this.modePanels.forEach(
            function (panel) {
                var shouldShow = panel.getAttribute("data-mr-demo-view") === this.panelMode;
                panel.hidden = !shouldShow;
            }.bind(this)
        );
        text(this.modeSummary, this.panelModeCopy[this.panelMode]);
    };

    Demo.prototype.seed = function () {
        this.pushTimeline("Cluster boot");
        this.pushLog("Boot", "3-node local cluster started.");
        this.pushTimeline("Leader elected");
        this.pushLog("Election", "node-b becomes leader for term 1.");
        this.pushTimeline("Initial commit");
        this.pushLog("Commit", "Initial value committed at index 1.");
        this.explainerHeading = "Cluster is healthy and ready for a new client command.";
        this.explainerText =
            "A healthy leader can accept writes, replicate them to followers, and commit once quorum acknowledges the entry.";
        this.flowItems = [
            "Client sends a command to the current leader.",
            "Leader appends the entry to its local log.",
            "Leader replicates the entry to followers and waits for quorum.",
            "Once quorum acknowledges, the entry is committed.",
        ];
        this.renderTour();
    };

    Demo.prototype.pushLog = function (tag, message) {
        this.logItems.unshift({ tag: tag, message: message });
        this.logItems = this.logItems.slice(0, 8);
    };

    Demo.prototype.pushTimeline = function (label) {
        var time = new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" });
        this.timelineItems.unshift({ label: label, time: time });
        this.timelineItems = this.timelineItems.slice(0, 6);
    };

    Demo.prototype.startTour = function () {
        this.clearTourTimer();
        this.reset();
        this.tourActive = true;
        this.tourAutoplay = false;
        this.tourIndex = 0;
        this.pushLog("Tour", "Guided tour started.");
        this.render();
    };

    Demo.prototype.startAutoplayTour = function () {
        this.clearTourTimer();
        this.reset();
        this.tourActive = true;
        this.tourAutoplay = true;
        this.tourIndex = 0;
        this.pushLog("Tour", "Autoplay tour started.");
        this.render();
        this.showUpcomingStep();
    };

    Demo.prototype.advanceTour = function () {
        if (!this.tourActive || this.tourIndex < 0 || this.tourIndex >= this.tourSteps.length) {
            return;
        }

        var step = this.tourSteps[this.tourIndex];
        if (step && typeof step.action === "function") {
            step.action.call(this);
        }

        if (this.tourIndex >= this.tourSteps.length - 1) {
            this.tourIndex = this.tourSteps.length;
            this.pushLog("Tour", "Guided tour completed.");
        } else {
            this.tourIndex += 1;
        }

        this.render();
        if (this.tourAutoplay) {
            this.scheduleAutoplayStep();
        }
    };

    Demo.prototype.stopTour = function () {
        if (!this.tourActive && this.tourIndex < 0) {
            return;
        }

        this.clearTourTimer();
        this.tourActive = false;
        this.tourAutoplay = false;
        this.tourIndex = -1;
        this.pushLog("Tour", "Guided tour exited.");
        this.render();
    };

    Demo.prototype.scheduleAutoplayStep = function () {
        var self = this;
        this.clearTourTimer();
        if (!this.tourAutoplay || !this.tourActive || this.tourIndex < 0 || this.tourIndex >= this.tourSteps.length) {
            return;
        }
        this.tourTimer = window.setTimeout(function () {
            self.advanceTour();
        }, this.tourDelayMs);
    };

    Demo.prototype.showUpcomingStep = function () {
        var self = this;
        this.clearTourTimer();
        if (!this.tourAutoplay || !this.tourActive || this.tourIndex < 0 || this.tourIndex >= this.tourSteps.length) {
            return;
        }

        var step = this.tourSteps[this.tourIndex];
        if (this.tourNextHint && step) {
            this.tourNextHint.hidden = false;
            text(this.tourNextHint, "Coming next: " + step.title.replace(/^Step \d+:\s*/, "").toLowerCase() + ".");
        }

        this.tourTimer = window.setTimeout(function () {
            if (self.tourNextHint) {
                self.tourNextHint.hidden = true;
            }
            self.advanceTour();
        }, Math.max(1200, Math.round(this.tourDelayMs * 0.42)));
    };

    Demo.prototype.clearTourTimer = function () {
        if (this.tourTimer) {
            window.clearTimeout(this.tourTimer);
            this.tourTimer = null;
        }
    };

    Demo.prototype.renderTour = function () {
        if (
            !this.tourTitle ||
            !this.tourStep ||
            !this.tourBody ||
            !this.tourNextHint ||
            !this.tourStartButton ||
            !this.tourAutoButton ||
            !this.tourNextButton ||
            !this.tourStopButton
        ) {
            return;
        }

        if (!this.tourActive && this.tourIndex < 0) {
            text(this.tourTitle, "Walk the cluster step by step");
            text(this.tourStep, "Not started");
            text(
                this.tourBody,
                "Start the guided tour to see a healthy write, follower loss, continued quorum, recovery, and leader change in the shortest possible order."
            );
            this.tourStartButton.disabled = false;
            this.tourStartButton.textContent = "Start tour";
            this.tourAutoButton.disabled = false;
            this.tourAutoButton.textContent = "Autoplay tour";
            this.tourNextButton.disabled = true;
            this.tourNextButton.textContent = "Next step";
            this.tourStopButton.disabled = true;
            this.tourNextHint.hidden = true;
            return;
        }

        if (this.tourIndex >= this.tourSteps.length) {
            text(this.tourTitle, "Tour complete");
            text(this.tourStep, this.tourSteps.length + "/" + this.tourSteps.length);
            text(
                this.tourBody,
                "You have walked through healthy replication, follower loss, continued quorum, recovery, and a leader change. Restart the tour or keep exploring manually."
            );
            this.tourActive = false;
            this.tourAutoplay = false;
            this.clearTourTimer();
            this.tourStartButton.disabled = false;
            this.tourStartButton.textContent = "Restart tour";
            this.tourAutoButton.disabled = false;
            this.tourAutoButton.textContent = "Autoplay tour";
            this.tourNextButton.disabled = true;
            this.tourNextButton.textContent = "Next step";
            this.tourStopButton.disabled = false;
            this.tourNextHint.hidden = true;
            return;
        }

        var step = this.tourSteps[this.tourIndex];
        text(this.tourTitle, step.title);
        text(this.tourStep, this.tourIndex + 1 + "/" + this.tourSteps.length);
        text(this.tourBody, step.body);
        this.tourStartButton.disabled = true;
        this.tourStartButton.textContent = "Start tour";
        this.tourAutoButton.disabled = this.tourAutoplay;
        this.tourAutoButton.textContent = this.tourAutoplay ? "Autoplaying..." : "Autoplay tour";
        this.tourNextButton.disabled = this.tourAutoplay;
        this.tourNextButton.textContent = this.tourIndex === this.tourSteps.length - 1 ? "Finish tour" : "Next step";
        this.tourStopButton.disabled = false;
        if (!this.tourAutoplay) {
            this.tourNextHint.hidden = true;
        }
    };

    Demo.prototype.nodeById = function (id) {
        for (var i = 0; i < this.nodes.length; i += 1) {
            if (this.nodes[i].id === id) {
                return this.nodes[i];
            }
        }
        return null;
    };

    Demo.prototype.electNextLeader = function () {
        this.term += 1;
        var available = this.nodes.filter(function (node) {
            return node.kind === "raft" && !node.isolated;
        });
        if (!available.length) {
            return;
        }

        var currentIndex = 0;
        for (var i = 0; i < available.length; i += 1) {
            if (available[i].id === this.leaderId) {
                currentIndex = i;
                break;
            }
        }

        var nextLeader = available[(currentIndex + 1) % available.length];
        this.leaderId = nextLeader.id;
        this.lastOutcome = "Leader rotated to " + nextLeader.name;
        this.pushTimeline("Term " + this.term + " election");
        for (var j = 0; j < available.length; j += 1) {
            if (available[j].id !== nextLeader.id) {
                this.emitParticle(available[j].id, nextLeader.id, "vote", "election");
            }
        }
        this.flowItems = [
            nextLeader.name + " starts a new election round.",
            "Healthy followers vote in the new term.",
            "The candidate with quorum becomes leader.",
            "Clients should now send writes to " + nextLeader.name + ".",
        ];
        this.explainerHeading = nextLeader.name + " is now the write entry point.";
        this.explainerText =
            "Leader changes are normal in Raft. What matters is that a majority can still communicate and elect a single new leader for the new term.";
        this.pushLog("Election", nextLeader.name + " becomes leader for term " + this.term + ".");
        this.render();
    };

    Demo.prototype.replicate = function () {
        var leader = this.nodeById(this.leaderId);
        if (!leader || leader.isolated) {
            this.pushTimeline("Replication blocked");
            this.lastOutcome = "No quorum-backed leader available";
            this.flowItems = [
                "Client sends a command, but no healthy leader can accept it.",
                "Replication cannot begin without a leader.",
                "The cluster must recover communication or elect a healthy leader.",
            ];
            this.explainerHeading = "Replication is blocked because the cluster cannot rely on a healthy leader.";
            this.explainerText =
                "Without a reachable leader backed by quorum, a write cannot be safely accepted. Recovery or a new election must happen first.";
            this.pushLog("Blocked", "No healthy leader can replicate the command.");
            this.render();
            return;
        }

        this.commitIndex += 1;
        var healthyFollowers = 0;
        this.emitParticle("client", this.leaderId, this.pendingValue.indexOf("get") === 0 ? "query" : "write", "client");
        for (var i = 0; i < this.nodes.length; i += 1) {
            if (this.nodes[i].kind === "raft" && !this.nodes[i].isolated) {
                this.nodes[i].logIndex = this.commitIndex;
                if (this.nodes[i].id !== this.leaderId) {
                    healthyFollowers += 1;
                }
            }
        }

        this.pushLog(
            "Replicate",
            leader.name + " commits \"" + this.pendingValue + "\" at index " + this.commitIndex + " with " + healthyFollowers + " follower acknowledgement(s)."
        );
        this.pushTimeline("Commit index " + this.commitIndex);
        for (var j = 0; j < this.nodes.length; j += 1) {
            if (this.nodes[j].kind === "raft" && this.nodes[j].id !== this.leaderId && !this.nodes[j].isolated) {
                this.emitParticle(this.leaderId, this.nodes[j].id, "append", "replication");
                this.emitParticle(this.nodes[j].id, this.leaderId, "ack", "replication");
            }
        }
        this.emitParticle(this.leaderId, "client", "commit", "client");
        this.lastOutcome = "Committed at index " + this.commitIndex;
        this.flowItems = [
            "Client sends \"" + this.pendingValue + "\" to " + leader.name + ".",
            leader.name + " appends the entry locally at index " + this.commitIndex + ".",
            healthyFollowers > 0
                ? "Followers acknowledge the entry and quorum is satisfied."
                : "Leader has no follower acknowledgements, so this would be unsafe in a real cluster.",
            "The cluster advances commit index to " + this.commitIndex + ".",
        ];
        this.explainerHeading = "The command is committed because a majority acknowledged it.";
        this.explainerText =
            healthyFollowers > 0
                ? "This is the core Raft write path: leader append, follower acknowledgements, then a safe commit visible to the cluster."
                : "A leader-only append is not enough for a safe commit in a real Raft cluster. Majority acknowledgement is the part that matters.";
        this.render();
    };

    Demo.prototype.partition = function () {
        if (this.partitioned) {
            return;
        }
        var node = this.nodeById("node-c");
        node.isolated = true;
        this.partitioned = true;
        this.pushTimeline("Follower partition");
        this.lastOutcome = "node-c is isolated; quorum remains available";
        this.flowItems = [
            "Network connectivity to node-c is cut.",
            "The other two nodes still form a majority.",
            "The leader can continue committing entries with quorum.",
            "node-c will need to catch up after recovery.",
        ];
        this.explainerHeading = "One follower is gone, but the cluster still has quorum.";
        this.explainerText =
            "Raft tolerates follower loss as long as a majority remains. Reads and writes can continue through the leader and the remaining follower.";
        this.pushLog("Network", "node-c is partitioned from the rest of the cluster.");
        this.render();
    };

    Demo.prototype.recover = function () {
        var node = this.nodeById("node-c");
        node.isolated = false;
        node.logIndex = this.commitIndex;
        this.partitioned = false;
        this.pushTimeline("Follower recovered");
        this.emitParticle(this.leaderId, "node-c", "catch-up", "recovery");
        this.lastOutcome = "node-c caught up to commit index " + this.commitIndex;
        this.flowItems = [
            "node-c reconnects to the leader and healthy follower.",
            "The leader sends any missing log entries or snapshots.",
            "node-c applies the missing state and reaches commit index " + this.commitIndex + ".",
            "All three nodes are healthy again.",
        ];
        this.explainerHeading = "The recovered follower catches up and rejoins the healthy majority.";
        this.explainerText =
            "Recovery is not just reconnecting the node. The lagging follower must receive missing entries or snapshots until its committed state matches the cluster.";
        this.pushLog("Recover", "node-c reconnects and catches up to commit index " + this.commitIndex + ".");
        this.render();
    };

    Demo.prototype.reset = function () {
        this.clearTourTimer();
        this.term = 1;
        this.commitIndex = 1;
        this.leaderId = "node-b";
        this.partitioned = false;
        this.pendingValue = "set value=42";
        this.logItems = [];
        this.timelineItems = [];
        this.flowItems = [];
        this.tourAutoplay = false;
        this.lastOutcome = "Ready to replicate";
        this.commandSelect.value = this.pendingValue;
        for (var i = 0; i < this.nodes.length; i += 1) {
            this.nodes[i].isolated = false;
            if (this.nodes[i].kind === "raft") {
                this.nodes[i].logIndex = this.nodes[i].id === "node-b" ? 1 : 0;
            }
        }
        this.seed();
        this.render();
    };

    Demo.prototype.runHappyPath = function () {
        if (this.partitioned) {
            this.recover();
        }
        this.pendingValue = "set value=42";
        this.commandSelect.value = this.pendingValue;
        this.pushLog("Scenario", "Running happy-path replication.");
        this.replicate();
    };

    Demo.prototype.runPartitionScenario = function () {
        if (!this.partitioned) {
            this.partition();
        }
        this.pendingValue = "compare-and-set value=42->84";
        this.commandSelect.value = this.pendingValue;
        this.pushLog("Scenario", "Leader keeps quorum and commits while node-c is isolated.");
        this.replicate();
    };

    Demo.prototype.runRecoveryScenario = function () {
        if (!this.partitioned) {
            this.partition();
            this.pendingValue = "get value";
            this.commandSelect.value = this.pendingValue;
            this.replicate();
        }
        this.recover();
        this.pushLog("Scenario", "Recovered follower catches up to the leader's commit index.");
        this.render();
    };

    Demo.prototype.render = function () {
        for (var i = 0; i < this.nodes.length; i += 1) {
            var node = this.nodes[i];
            var card = byId(node.id);
            card.className = "mr-node";
            if (node.kind === "client") {
                card.className += " is-client";
            } else {
                card.className += " " + (node.id === this.leaderId ? "is-leader" : "is-follower");
            }
            if (node.isolated) {
                card.className += " is-isolated";
            }
            text(card.querySelector(".mr-node-title"), node.name);
            text(card.querySelector(".mr-node-role"), node.kind === "client" ? "Client" : node.id === this.leaderId ? "Leader" : "Follower");
            text(
                card.querySelector(".mr-node-log"),
                node.kind === "client"
                    ? "sends commands and reads"
                    : "commit index " + node.logIndex + (node.isolated ? " • isolated" : "")
            );
        }

        text(this.termValue, String(this.term));
        text(this.leaderValue, this.leaderId);
        text(this.commitValue, String(this.commitIndex));
        text(this.commandView, this.pendingValue);
        this.renderQuorum();
        text(this.outcomeView, this.lastOutcome);
        text(this.explainerTitle, this.explainerHeading);
        text(this.explainerBody, this.explainerText);
        this.renderTour();

        this.logList.innerHTML = "";
        for (var j = 0; j < this.logItems.length; j += 1) {
            var item = document.createElement("li");
            item.innerHTML = '<span class="mr-log-tag">' + this.logItems[j].tag + '</span>' + this.logItems[j].message;
            this.logList.appendChild(item);
        }

        this.timelineList.innerHTML = "";
        for (var m = 0; m < this.timelineItems.length; m += 1) {
            var timelineItem = document.createElement("li");
            timelineItem.innerHTML = "<strong>" + this.timelineItems[m].time + "</strong> " + this.timelineItems[m].label;
            this.timelineList.appendChild(timelineItem);
        }

        this.flowList.innerHTML = "";
        for (var k = 0; k < this.flowItems.length; k += 1) {
            var flowItem = document.createElement("li");
            flowItem.textContent = this.flowItems[k];
            this.flowList.appendChild(flowItem);
        }

        this.positionNodes();
        this.renderLinks();
    };

    Demo.prototype.availableCount = function () {
        var count = 0;
        for (var i = 0; i < this.nodes.length; i += 1) {
            if (this.nodes[i].kind === "raft" && !this.nodes[i].isolated) {
                count += 1;
            }
        }
        return count;
    };

    Demo.prototype.renderQuorum = function () {
        var available = this.availableCount();
        var status = "Healthy quorum";
        var className = "mr-quorum-badge is-healthy";
        var detail = available + "/3 raft nodes available";

        if (available === 2) {
            status = "Majority intact";
            className = "mr-quorum-badge is-degraded";
            detail += " • one follower can be lost and writes still commit";
        } else if (available < 2) {
            status = "No quorum";
            className = "mr-quorum-badge is-lost";
            detail += " • writes are unsafe until recovery";
        }

        if (this.quorumBadge) {
            this.quorumBadge.className = className;
            text(this.quorumBadge, status);
        }
        text(this.quorumView, detail);
    };

    Demo.prototype.emitParticle = function (fromId, toId, label, type) {
        var from = byId(fromId);
        var to = byId(toId);
        if (!from || !to || !this.particleLayer) {
            return;
        }
        var clusterRect = this.cluster.getBoundingClientRect();
        var fromRect = from.getBoundingClientRect();
        var toRect = to.getBoundingClientRect();
        var particle = document.createElement("div");
        particle.className = "mr-particle is-" + type;
        particle.textContent = label;
        particle.style.left = fromRect.left + fromRect.width / 2 - clusterRect.left + "px";
        particle.style.top = fromRect.top + fromRect.height / 2 - clusterRect.top + "px";
        particle.style.setProperty("--mr-dx", toRect.left - fromRect.left + "px");
        particle.style.setProperty("--mr-dy", toRect.top - fromRect.top + "px");
        this.particleLayer.appendChild(particle);
        window.setTimeout(function () {
            particle.remove();
        }, 2800);
    };

    Demo.prototype.renderLinks = function () {
        var clusterRect = this.cluster.getBoundingClientRect();
        for (var i = 0; i < this.lines.length; i += 1) {
            var line = this.lines[i];
            var from = byId(line.getAttribute("data-from"));
            var to = byId(line.getAttribute("data-to"));
            var fromRect = from.getBoundingClientRect();
            var toRect = to.getBoundingClientRect();
            var fromNode = this.nodeById(line.getAttribute("data-from"));
            var toNode = this.nodeById(line.getAttribute("data-to"));

            line.setAttribute("x1", String(fromRect.left + fromRect.width / 2 - clusterRect.left));
            line.setAttribute("y1", String(fromRect.top + fromRect.height / 2 - clusterRect.top));
            line.setAttribute("x2", String(toRect.left + toRect.width / 2 - clusterRect.left));
            line.setAttribute("y2", String(toRect.top + toRect.height / 2 - clusterRect.top));

            var className = "mr-link";
            if (fromNode.isolated || toNode.isolated) {
                className += " is-cut";
            } else if (fromNode.id === this.leaderId || toNode.id === this.leaderId) {
                className += " is-active";
            }
            line.setAttribute("class", className);
        }
    };

    function HomeTabs() {
        this.root = bySelector("[data-mr-tabs]");
        if (!this.root) {
            return;
        }

        this.buttons = bySelectorAll("[data-tab-target]", this.root);
        this.panels = bySelectorAll("[data-tab-panel]", this.root);
        this.bind();
    }

    HomeTabs.prototype.bind = function () {
        var self = this;
        this.buttons.forEach(function (button) {
            button.addEventListener("click", function () {
                self.activate(button.getAttribute("data-tab-target"));
            });
        });
    };

    HomeTabs.prototype.activate = function (target) {
        this.buttons.forEach(function (button) {
            var isActive = button.getAttribute("data-tab-target") === target;
            button.classList.toggle("is-active", isActive);
            button.setAttribute("aria-selected", isActive ? "true" : "false");
        });

        this.panels.forEach(function (panel) {
            var isActive = panel.getAttribute("data-tab-panel") === target;
            panel.classList.toggle("is-active", isActive);
            panel.hidden = !isActive;
        });
    };

    document.addEventListener("DOMContentLoaded", function () {
        new SiteLayout();
        new NavEnhancements();
        new ReadingEnhancements();
        new Demo();
        new HomeTabs();
    });
})();
