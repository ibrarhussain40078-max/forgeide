package com.example.data.templates

import com.example.data.model.CodeFile
import com.example.data.model.FileLanguage
import com.example.data.model.IdeMode

object StarterTemplates {

    fun defaultProjectName(mode: IdeMode): String = when (mode) {
        IdeMode.WEBSITE -> "Nexus Studio Web"
        IdeMode.ANDROID -> "PulseKit Android"
    }

    val promptSuggestionsWebsite = listOf(
        "Build a scientific calculator",
        "Create an e-commerce store",
        "Interactive Kanban sprint board",
        "Crypto portfolio tracker"
    )

    val promptSuggestionsAndroid = listOf(
        "Build a scientific calculator",
        "Create an e-commerce store",
        "Fitness workout & rep counter",
        "Daily expense & budget tracker"
    )

    fun websiteStarterFiles(): List<CodeFile> = listOf(
        CodeFile(
            name = "index.html",
            language = FileLanguage.HTML,
            content = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Nexus Launchpad</title>
  <link rel="stylesheet" href="styles.css" />
</head>
<body>
  <div class="shell">
    <header class="topbar">
      <div class="brand">
        <span class="dot"></span>
        <span>Nexus Studio</span>
      </div>
      <div class="pill-group">
        <button class="pill active" data-accent="cyan">Cyan</button>
        <button class="pill" data-accent="indigo">Indigo</button>
        <button class="pill" data-accent="emerald">Emerald</button>
      </div>
    </header>

    <section class="hero">
      <span class="badge">LIVE PREVIEW</span>
      <h1>Ship web experiences faster.</h1>
      <p>Edit HTML, CSS, or JS on the left and watch this sandbox update live with console telemetry.</p>
      <div class="metrics">
        <div class="metric-card">
          <span class="metric-label">Active Tasks</span>
          <strong id="task-count">3</strong>
        </div>
        <div class="metric-card">
          <span class="metric-label">Completed</span>
          <strong id="done-count">1</strong>
        </div>
        <div class="metric-card">
          <span class="metric-label">Build Score</span>
          <strong id="score-val">98%</strong>
        </div>
      </div>
    </section>

    <section class="panel">
      <h2>Sprint Checklist</h2>
      <form id="task-form" class="task-form">
        <input id="task-input" type="text" placeholder="Add a launch milestone..." required />
        <button type="submit">Add</button>
      </form>
      <ul id="task-list" class="task-list"></ul>
    </section>
  </div>
  <script src="app.js"></script>
</body>
</html>"""
        ),
        CodeFile(
            name = "styles.css",
            language = FileLanguage.CSS,
            content = """:root {
  --bg: #080c14;
  --card: #111827;
  --border: #1f2d45;
  --text: #f1f5f9;
  --muted: #94a3b8;
  --accent: #38bdf8;
}

* { box-sizing: border-box; margin: 0; padding: 0; }

body {
  font-family: system-ui, -apple-system, sans-serif;
  background: radial-gradient(circle at top right, rgba(56, 189, 248, 0.14), transparent 45%), var(--bg);
  color: var(--text);
  min-height: 100vh;
  padding: 20px;
}

.shell { max-width: 820px; margin: 0 auto; display: flex; flex-direction: column; gap: 18px; }

.topbar {
  display: flex; flex-wrap: wrap; gap: 12px;
  justify-content: space-between; align-items: center;
  padding: 12px 16px; border-radius: 14px;
  background: var(--card); border: 1px solid var(--border);
}

.brand { display: flex; align-items: center; gap: 10px; font-weight: 700; letter-spacing: -0.02em; }
.dot { width: 10px; height: 10px; border-radius: 50%; background: var(--accent); box-shadow: 0 0 12px var(--accent); }

.pill-group { display: flex; gap: 8px; }
.pill {
  border: 1px solid var(--border); background: transparent; color: var(--muted);
  padding: 6px 12px; border-radius: 999px; font-size: 12px; cursor: pointer;
}
.pill.active { background: var(--accent); color: #06101d; border-color: var(--accent); font-weight: 700; }

.hero {
  padding: 24px; border-radius: 18px;
  background: linear-gradient(145deg, #111a2b, #0c1320);
  border: 1px solid var(--border);
}
.badge {
  display: inline-block; font-size: 10px; font-weight: 700; letter-spacing: 0.08em;
  color: var(--accent); background: rgba(56, 189, 248, 0.12);
  padding: 4px 10px; border-radius: 999px; margin-bottom: 10px;
}
.hero h1 { font-size: clamp(22px, 4vw, 32px); margin-bottom: 8px; }
.hero p { color: var(--muted); font-size: 14px; line-height: 1.5; margin-bottom: 18px; }

.metrics { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
@media (max-width: 520px) { .metrics { grid-template-columns: 1fr; } }

.metric-card {
  background: rgba(8, 12, 20, 0.7); border: 1px solid var(--border);
  border-radius: 12px; padding: 12px;
}
.metric-label { display: block; font-size: 11px; color: var(--muted); margin-bottom: 4px; }
.metric-card strong { font-size: 22px; color: var(--accent); }

.panel {
  background: var(--card); border: 1px solid var(--border);
  border-radius: 16px; padding: 18px;
}
.panel h2 { font-size: 16px; margin-bottom: 12px; }

.task-form { display: flex; gap: 8px; margin-bottom: 14px; }
.task-form input {
  flex: 1; padding: 10px 12px; border-radius: 10px;
  border: 1px solid var(--border); background: var(--bg); color: var(--text);
}
.task-form button {
  padding: 10px 16px; border-radius: 10px; border: none;
  background: var(--accent); color: #06101d; font-weight: 700; cursor: pointer;
}

.task-list { list-style: none; display: flex; flex-direction: column; gap: 8px; }
.task-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 12px; border-radius: 10px; background: var(--bg);
  border: 1px solid var(--border); cursor: pointer;
}
.task-item.done span { text-decoration: line-through; color: var(--muted); }"""
        ),
        CodeFile(
            name = "app.js",
            language = FileLanguage.JS,
            content = """const accents = {
  cyan: '#38bdf8',
  indigo: '#818cf8',
  emerald: '#34d399'
};

let tasks = [
  { id: 1, title: 'Design responsive hero grid', done: true },
  { id: 2, title: 'Wire Gemini code generation', done: false },
  { id: 3, title: 'Export production ZIP bundle', done: false }
];

const listEl = document.getElementById('task-list');
const formEl = document.getElementById('task-form');
const inputEl = document.getElementById('task-input');

function render() {
  listEl.innerHTML = '';
  tasks.forEach(task => {
    const li = document.createElement('li');
    li.className = 'task-item' + (task.done ? ' done' : '');
    li.innerHTML = '<span>' + task.title + '</span><small>' + (task.done ? 'Done' : 'Pending') + '</small>';
    li.onclick = () => {
      task.done = !task.done;
      console.log('Toggled task:', task.title, '-> done=' + task.done);
      render();
    };
    listEl.appendChild(li);
  });

  const done = tasks.filter(t => t.done).length;
  document.getElementById('task-count').textContent = tasks.length - done;
  document.getElementById('done-count').textContent = done;
  document.getElementById('score-val').textContent =
    Math.round((done / Math.max(1, tasks.length)) * 100) + '%';
}

document.querySelectorAll('.pill').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.pill').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    const color = accents[btn.dataset.accent];
    document.documentElement.style.setProperty('--accent', color);
    console.info('Switched theme accent to', btn.dataset.accent);
  });
});

formEl.addEventListener('submit', (e) => {
  e.preventDefault();
  const title = inputEl.value.trim();
  if (!title) return;
  tasks.unshift({ id: Date.now(), title, done: false });
  inputEl.value = '';
  console.log('Added milestone:', title);
  render();
});

console.log('Nexus Studio web runtime initialized.');
render();"""
        )
    )

    fun androidStarterFiles(): List<CodeFile> = listOf(
        CodeFile(
            name = "MainActivity.kt",
            language = FileLanguage.KOTLIN,
            content = """package com.pulsekit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                PulseHomeScreen()
            }
        }
    }
}

@Composable
fun PulseHomeScreen() {
    var focusMinutes by remember { mutableIntStateOf(25) }
    var deepWorkEnabled by remember { mutableStateOf(true) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("PulseKit Focus", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Session Target: ${'$'}focusMinutes min")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { focusMinutes += 5 }) { Text("+5 min") }
                        OutlinedButton(onClick = { if (focusMinutes > 5) focusMinutes -= 5 }) {
                            Text("-5 min")
                        }
                    }
                }
            }
        }
    }
}"""
        ),
        CodeFile(
            name = "AndroidManifest.xml",
            language = FileLanguage.XML,
            content = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="true"
        android:label="PulseKit"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>"""
        ),
        CodeFile(
            name = "build.gradle.kts",
            language = FileLanguage.GRADLE,
            content = """plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.pulsekit.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pulsekit.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.10.1")
}"""
        ),
        CodeFile(
            name = "ui-preview.json",
            language = FileLanguage.JSON,
            content = """{
  "appName": "PulseKit Focus",
  "primaryColor": "#38BDF8",
  "startScreen": "home",
  "initialState": {
    "minutes": 25,
    "sessions": 4,
    "deepWork": true,
    "noteInput": ""
  },
  "initialLists": {
    "habits": [
      "Morning code review",
      "Ship UI simulator",
      "Hydrate & stretch"
    ]
  },
  "screens": {
    "home": {
      "title": "PulseKit Focus",
      "elements": [
        {
          "type": "heroCard",
          "badge": "ACTIVE SPRINT",
          "title": "{{minutes}} min",
          "subtitle": "Completed {{sessions}} deep-work blocks today"
        },
        {
          "type": "row",
          "children": [
            { "type": "button", "label": "+5 Min", "action": "increment", "key": "minutes", "step": 5 },
            { "type": "outlinedButton", "label": "-5 Min", "action": "decrement", "key": "minutes", "step": 5 },
            { "type": "outlinedButton", "label": "Log +1", "action": "increment", "key": "sessions", "step": 1 }
          ]
        },
        {
          "type": "switchRow",
          "label": "Do Not Disturb Mode",
          "key": "deepWork"
        },
        {
          "type": "textField",
          "placeholder": "Add focus goal...",
          "key": "noteInput"
        },
        {
          "type": "button",
          "label": "Add Goal to Sprint",
          "action": "addListItem",
          "listKey": "habits",
          "inputKey": "noteInput"
        },
        {
          "type": "list",
          "title": "Today's Checklist",
          "listKey": "habits"
        },
        {
          "type": "outlinedButton",
          "label": "Open Analytics Screen",
          "action": "navigate",
          "target": "stats"
        }
      ]
    },
    "stats": {
      "title": "Focus Analytics",
      "elements": [
        {
          "type": "heroCard",
          "badge": "WEEKLY VELOCITY",
          "title": "{{sessions}} Sessions",
          "subtitle": "Target duration: {{minutes}} minutes per block"
        },
        {
          "type": "metricRow",
          "metrics": [
            { "label": "Streak", "value": "6 Days" },
            { "label": "Focus", "value": "94%" }
          ]
        },
        {
          "type": "button",
          "label": "Reset Daily Counter",
          "action": "set",
          "key": "sessions",
          "value": 0
        },
        {
          "type": "outlinedButton",
          "label": "Back to Home",
          "action": "back"
        }
      ]
    }
  }
}"""
        )
    )
}
