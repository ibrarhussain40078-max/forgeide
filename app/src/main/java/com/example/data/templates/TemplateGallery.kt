package com.example.data.templates

import com.example.data.model.CodeFile
import com.example.data.model.FileLanguage
import com.example.data.model.IdeMode

data class IdeTemplateItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val mode: IdeMode,
    val projectName: String,
    val files: List<CodeFile>
)

object TemplateGallery {

    val allTemplates: List<IdeTemplateItem> by lazy {
        listOf(
            portfolioWebsiteTemplate(),
            storefrontWebsiteTemplate(),
            kotlinLudoGameTemplate(),
            todoAndroidTemplate()
        )
    }

    private fun portfolioWebsiteTemplate() = IdeTemplateItem(
        id = "portfolio_web",
        title = "Portfolio Website",
        subtitle = "Responsive dev portfolio with project filter & contact telemetry",
        mode = IdeMode.WEBSITE,
        projectName = "Aria Vance Portfolio",
        files = listOf(
            CodeFile(
                name = "index.html",
                language = FileLanguage.HTML,
                content = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Aria Vance — Creative Engineer</title>
  <link rel="stylesheet" href="styles.css" />
</head>
<body>
  <main class="container">
    <header class="hero">
      <span class="tag">AVAILABLE FOR SELECT WORK</span>
      <h1>Aria Vance</h1>
      <p>Full-stack Systems & Mobile Product Engineer building developer tools and tactile interfaces.</p>
      <div class="filters">
        <button class="chip active" data-cat="all">All</button>
        <button class="chip" data-cat="web">Web</button>
        <button class="chip" data-cat="mobile">Android</button>
      </div>
    </header>
    <section id="projects" class="grid"></section>
    <section class="contact-card">
      <h2>Quick Inquiry</h2>
      <form id="contact-form">
        <input id="sender-email" type="email" placeholder="you@company.com" required />
        <button type="submit">Send Hello</button>
      </form>
      <p id="contact-status" class="status"></p>
    </section>
  </main>
  <script src="app.js"></script>
</body>
</html>"""
            ),
            CodeFile(
                name = "styles.css",
                language = FileLanguage.CSS,
                content = """* { box-sizing: border-box; margin: 0; padding: 0; }
body {
  font-family: system-ui, sans-serif;
  background: #070b14;
  color: #f1f5f9;
  padding: 20px;
}
.container { max-width: 840px; margin: 0 auto; display: flex; flex-direction: column; gap: 20px; }
.hero { padding: 24px; border-radius: 18px; background: linear-gradient(135deg, #0f172a, #172554); border: 1px solid #1e293b; }
.tag { font-size: 10px; font-weight: 700; letter-spacing: 0.08em; color: #38bdf8; }
.hero h1 { font-size: 30px; margin: 8px 0; }
.hero p { color: #94a3b8; font-size: 14px; line-height: 1.5; margin-bottom: 16px; }
.filters { display: flex; gap: 8px; flex-wrap: wrap; }
.chip { border: 1px solid #334155; background: #0f172a; color: #cbd5e1; padding: 6px 14px; border-radius: 999px; cursor: pointer; }
.chip.active { background: #38bdf8; color: #06101d; border-color: #38bdf8; font-weight: 700; }
.grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; }
.card { background: #0f172a; border: 1px solid #1e293b; border-radius: 14px; padding: 16px; }
.card small { color: #818cf8; font-weight: 700; text-transform: uppercase; font-size: 10px; }
.card h3 { margin: 6px 0; font-size: 16px; }
.card p { color: #94a3b8; font-size: 13px; }
.contact-card { background: #0f172a; border: 1px solid #1e293b; border-radius: 14px; padding: 18px; }
.contact-card h2 { font-size: 16px; margin-bottom: 10px; }
#contact-form { display: flex; gap: 8px; }
#contact-form input { flex: 1; padding: 10px; border-radius: 8px; border: 1px solid #334155; background: #070b14; color: #fff; }
#contact-form button { padding: 10px 16px; border-radius: 8px; border: none; background: #38bdf8; color: #06101d; font-weight: 700; cursor: pointer; }
.status { margin-top: 8px; color: #34d399; font-size: 12px; }"""
            ),
            CodeFile(
                name = "app.js",
                language = FileLanguage.JS,
                content = """const items = [
  { title: 'ForgeIDE Cloud Compiler', cat: 'mobile', desc: 'Kotlin + Compose mobile developer studio.' },
  { title: 'Pulse Telemetry Grid', cat: 'web', desc: 'Sub-millisecond WebSocket observability dashboard.' },
  { title: 'Aether Design Tokens', cat: 'web', desc: 'Cross-platform Material & CSS token pipeline.' }
];
const grid = document.getElementById('projects');
function render(cat) {
  grid.innerHTML = '';
  items.filter(i => cat === 'all' || i.cat === cat).forEach(item => {
    const el = document.createElement('div');
    el.className = 'card';
    el.innerHTML = '<small>' + item.cat + '</small><h3>' + item.title + '</h3><p>' + item.desc + '</p>';
    grid.appendChild(el);
  });
}
document.querySelectorAll('.chip').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.chip').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    console.log('Filtered portfolio by:', btn.dataset.cat);
    render(btn.dataset.cat);
  });
});
document.getElementById('contact-form').addEventListener('submit', e => {
  e.preventDefault();
  const email = document.getElementById('sender-email').value;
  document.getElementById('contact-status').textContent = 'Message queued for ' + email;
  console.info('Portfolio inquiry submitted from', email);
});
render('all');"""
            )
        )
    )

    private fun storefrontWebsiteTemplate() = IdeTemplateItem(
        id = "storefront_web",
        title = "Storefront App",
        subtitle = "E-commerce storefront with live cart & checkout calculator",
        mode = IdeMode.WEBSITE,
        projectName = "Nova Gear Storefront",
        files = listOf(
            CodeFile(
                name = "index.html",
                language = FileLanguage.HTML,
                content = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Nova Gear Store</title>
  <link rel="stylesheet" href="styles.css" />
</head>
<body>
  <div class="wrap">
    <header class="nav">
      <strong>NOVA GEAR</strong>
      <div class="cart-pill">Cart: <span id="cart-count">0</span> • $<span id="cart-total">0</span></div>
    </header>
    <section id="catalog" class="catalog"></section>
    <footer class="checkout">
      <button id="checkout-btn">Checkout Now</button>
      <span id="order-msg"></span>
    </footer>
  </div>
  <script src="app.js"></script>
</body>
</html>"""
            ),
            CodeFile(
                name = "styles.css",
                language = FileLanguage.CSS,
                content = """* { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: system-ui, sans-serif; background: #090d16; color: #f8fafc; padding: 18px; }
.wrap { max-width: 780px; margin: 0 auto; display: flex; flex-direction: column; gap: 16px; }
.nav { display: flex; justify-content: space-between; align-items: center; padding: 14px 16px; background: #111827; border: 1px solid #1f2937; border-radius: 14px; }
.cart-pill { background: rgba(56,189,248,0.14); color: #38bdf8; padding: 6px 12px; border-radius: 999px; font-weight: 700; font-size: 13px; }
.catalog { display: grid; grid-template-columns: repeat(auto-fit, minmax(210px, 1fr)); gap: 12px; }
.product { background: #111827; border: 1px solid #1f2937; border-radius: 14px; padding: 16px; display: flex; flex-direction: column; gap: 8px; }
.price { font-size: 20px; font-weight: 800; color: #34d399; }
.product button, .checkout button { padding: 10px; border-radius: 10px; border: none; background: #38bdf8; color: #06101d; font-weight: 700; cursor: pointer; }
.checkout { display: flex; align-items: center; gap: 12px; background: #111827; border: 1px solid #1f2937; border-radius: 14px; padding: 14px; }
#order-msg { color: #34d399; font-size: 13px; }"""
            ),
            CodeFile(
                name = "app.js",
                language = FileLanguage.JS,
                content = """const products = [
  { id: 1, name: 'Mechanical Keycap Kit', price: 69, desc: 'PBT double-shot dark obsidian' },
  { id: 2, name: 'USB-C Studio Dock', price: 129, desc: 'Dual 4K HDMI + 100W PD' },
  { id: 3, name: 'Desk Mat XL', price: 34, desc: 'Micro-weave precision surface' }
];
let count = 0;
let total = 0;
const catalog = document.getElementById('catalog');
products.forEach(p => {
  const card = document.createElement('div');
  card.className = 'product';
  card.innerHTML = '<h3>' + p.name + '</h3><small style="color:#94a3b8">' + p.desc + '</small><div class="price">$' + p.price + '</div><button>Add to Cart</button>';
  card.querySelector('button').onclick = () => {
    count++;
    total += p.price;
    document.getElementById('cart-count').textContent = count;
    document.getElementById('cart-total').textContent = total;
    console.log('Added', p.name, '| Total: $' + total);
  };
  catalog.appendChild(card);
});
document.getElementById('checkout-btn').onclick = () => {
  if (count === 0) { console.warn('Cart is empty!'); return; }
  document.getElementById('order-msg').textContent = 'Order placed for ' + count + ' items ($' + total + ')';
  console.info('Checkout completed:', { count, total });
};"""
            )
        )
    )

    private fun kotlinLudoGameTemplate() = IdeTemplateItem(
        id = "ludo_android",
        title = "Kotlin Ludo Starter",
        subtitle = "Interactive dice arena & turn tracker in Jetpack Compose",
        mode = IdeMode.ANDROID,
        projectName = "Royal Ludo Arena",
        files = listOf(
            CodeFile(
                name = "MainActivity.kt",
                language = FileLanguage.KOTLIN,
                content = """package com.forgeide.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                LudoArenaScreen()
            }
        }
    }
}

@Composable
fun LudoArenaScreen() {
    var diceValue by remember { mutableIntStateOf(6) }
    var redScore by remember { mutableIntStateOf(12) }
    var blueScore by remember { mutableIntStateOf(8) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Royal Ludo Arena", style = MaterialTheme.typography.headlineMedium)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Last Dice Roll: ${'$'}diceValue", style = MaterialTheme.typography.titleLarge)
                    Text("Red Steps: ${'$'}redScore | Blue Steps: ${'$'}blueScore")
                }
            }
            Button(onClick = {
                diceValue = Random.nextInt(1, 7)
                redScore += diceValue
            }) {
                Text("Roll Dice & Advance")
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
        android:label="Royal Ludo"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.NoActionBar">
        <activity android:name=".MainActivity" android:exported="true">
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
    namespace = "com.forgeide.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.forgeide.ludo"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { compose = true }
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
  "appName": "Royal Ludo Arena",
  "primaryColor": "#FBBF24",
  "startScreen": "home",
  "initialState": {
    "dice": 6,
    "redSteps": 14,
    "blueSteps": 9,
    "turn": "Red Player"
  },
  "initialLists": {
    "moves": [
      "Red rolled 6 -> Token #1 exited base",
      "Blue rolled 4 -> Advanced to tile 9"
    ]
  },
  "screens": {
    "home": {
      "title": "Royal Ludo Arena",
      "elements": [
        {
          "type": "heroCard",
          "badge": "ACTIVE MATCH",
          "title": "Dice: {{dice}}",
          "subtitle": "Current turn: {{turn}}"
        },
        {
          "type": "metricRow",
          "metrics": [
            { "label": "Red Steps", "value": "{{redSteps}}" },
            { "label": "Blue Steps", "value": "{{blueSteps}}" }
          ]
        },
        {
          "type": "row",
          "children": [
            { "type": "button", "label": "Roll Dice (Red)", "action": "rollDice", "key": "dice", "scoreKey": "redSteps", "turnKey": "turn", "nextTurn": "Blue Player" },
            { "type": "outlinedButton", "label": "Roll (Blue)", "action": "rollDice", "key": "dice", "scoreKey": "blueSteps", "turnKey": "turn", "nextTurn": "Red Player" }
          ]
        },
        {
          "type": "list",
          "title": "Match Move Log",
          "listKey": "moves"
        },
        {
          "type": "outlinedButton",
          "label": "Match Rules",
          "action": "navigate",
          "target": "rules"
        }
      ]
    },
    "rules": {
      "title": "Ludo Rules",
      "elements": [
        {
          "type": "heroCard",
          "badge": "GAMEPLAY",
          "title": "Roll 6 to Unlock",
          "subtitle": "Advance tokens around the 52-tile track and reach Home Triangle first."
        },
        {
          "type": "outlinedButton",
          "label": "Back to Board",
          "action": "back"
        }
      ]
    }
  }
}"""
            )
        )
    )

    private fun todoAndroidTemplate() = IdeTemplateItem(
        id = "todo_android",
        title = "Todo Android App",
        subtitle = "Material 3 task manager ready for live preview & APK build",
        mode = IdeMode.ANDROID,
        projectName = "TaskFlow Android",
        files = listOf(
            CodeFile(
                name = "MainActivity.kt",
                language = FileLanguage.KOTLIN,
                content = """package com.forgeide.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                TodoScreen()
            }
        }
    }
}

@Composable
fun TodoScreen() {
    var draft by remember { mutableStateOf("") }
    val tasks = remember { mutableStateListOf("Review PR #42", "Publish Debug APK") }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("TaskFlow", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text("New Task") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                if (draft.isNotBlank()) {
                    tasks.add(0, draft.trim())
                    draft = ""
                }
            }) {
                Text("Add Task")
            }
            LazyColumn {
                items(tasks) { item ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(item, modifier = Modifier.padding(14.dp))
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
        android:label="TaskFlow"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.NoActionBar">
        <activity android:name=".MainActivity" android:exported="true">
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
    namespace = "com.forgeide.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.forgeide.taskflow"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { compose = true }
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
  "appName": "TaskFlow Todo",
  "primaryColor": "#34D399",
  "startScreen": "home",
  "initialState": {
    "draft": "",
    "completedCount": 3,
    "reminders": true
  },
  "initialLists": {
    "todos": [
      "Wire GitHub Actions APK pipeline",
      "Test AI Error Fixer patch",
      "Ship release notes"
    ]
  },
  "screens": {
    "home": {
      "title": "TaskFlow",
      "elements": [
        {
          "type": "heroCard",
          "badge": "PRODUCTIVITY",
          "title": "{{completedCount}} Completed",
          "subtitle": "Tap tasks below or add a new item"
        },
        {
          "type": "textField",
          "placeholder": "Write a new task...",
          "key": "draft"
        },
        {
          "type": "row",
          "children": [
            { "type": "button", "label": "Add Task", "action": "addListItem", "listKey": "todos", "inputKey": "draft" },
            { "type": "outlinedButton", "label": "Complete +1", "action": "increment", "key": "completedCount", "step": 1 }
          ]
        },
        {
          "type": "switchRow",
          "label": "Daily Morning Reminders",
          "key": "reminders"
        },
        {
          "type": "list",
          "title": "Active Queue",
          "listKey": "todos"
        }
      ]
    }
  }
}"""
            )
        )
    )
}
