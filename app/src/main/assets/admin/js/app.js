document.addEventListener('DOMContentLoaded', function() {
    // Tab切换
    const tabs = document.querySelectorAll('.tab');
    const contents = document.querySelectorAll('.tab-content');
    
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            tabs.forEach(t => t.classList.remove('active'));
            contents.forEach(c => c.classList.remove('active'));
            
            tab.classList.add('active');
            const target = tab.dataset.tab;
            document.getElementById(target + '-tab').classList.add('active');
            
            if (target === 'leveldb') loadKVList();
            if (target === 'status') loadStatus();
        });
    });
    
    // SQLite执行
    document.getElementById('btnExecute').addEventListener('click', executeSQL);
    document.getElementById('btnClear').addEventListener('click', () => {
        document.getElementById('sqlInput').value = '';
        document.getElementById('sqlParams').value = '';
        document.getElementById('sqliteResult').innerHTML = '';
    });
    
    document.getElementById('quickSQL').addEventListener('change', function() {
        if (this.value) {
            document.getElementById('sqlInput').value = this.value;
            this.value = '';
        }
    });
    
    // LevelDB操作
    document.getElementById('btnSearch').addEventListener('click', loadKVList);
    document.getElementById('btnAddKV').addEventListener('click', addOrUpdateKV);
    
    // 检查服务状态
    checkStatus();
});

function checkStatus() {
    // 简单的状态检查 - 尝试访问API
    fetch('/sqlite/execute', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({sql: 'SELECT 1'})
    })
    .then(res => {
        if (res.ok) {
            document.getElementById('status').textContent = '✅ 运行中';
            document.getElementById('status').style.background = 'rgba(76,175,80,0.3)';
        }
    })
    .catch(() => {
        document.getElementById('status').textContent = '❌ 连接失败';
        document.getElementById('status').style.background = 'rgba(244,67,54,0.3)';
    });
}

function executeSQL() {
    const sql = document.getElementById('sqlInput').value.trim();
    if (!sql) return alert('请输入SQL语句');
    
    const paramsStr = document.getElementById('sqlParams').value.trim();
    let params = null;
    if (paramsStr) {
        try {
            params = JSON.parse(paramsStr);
        } catch (e) {
            return alert('参数JSON格式错误');
        }
    }
    
    const resultDiv = document.getElementById('sqliteResult');
    resultDiv.innerHTML = '<p>执行中...</p>';
    
    fetch('/sqlite/execute', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({sql, params})
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            if (Array.isArray(data.data)) {
                renderTable(data.data, resultDiv);
            } else {
                resultDiv.innerHTML = `<p>✅ ${data.message} (影响行数: ${data.rows_affected})</p>`;
            }
        } else {
            resultDiv.innerHTML = `<p class="error">❌ ${data.message}</p>`;
        }
    })
    .catch(err => {
        resultDiv.innerHTML = `<p class="error">❌ 请求失败: ${err.message}</p>`;
    });
}

function renderTable(data, container) {
    if (data.length === 0) {
        container.innerHTML = '<p>查询结果为空</p>';
        return;
    }
    
    const headers = Object.keys(data[0]);
    let html = `<p>查询结果 (${data.length} 条):</p><table><thead><tr>`;
    headers.forEach(h => html += `<th>${h}</th>`);
    html += '</tr></thead><tbody>';
    
    data.forEach(row => {
        html += '<tr>';
        headers.forEach(h => {
            const val = row[h];
            html += `<td>${val !== null ? String(val) : 'NULL'}</td>`;
        });
        html += '</tr>';
    });
    
    html += '</tbody></table>';
    container.innerHTML = html;
}

function loadKVList() {
    const searchKey = document.getElementById('kvSearch').value.trim();
    const listDiv = document.getElementById('kvList');
    listDiv.innerHTML = '<p>加载中...</p>';
    
    // LevelDB不支持列表所有键，这里简化处理
    // 实际应该有一个遍历API
    listDiv.innerHTML = '<p>提示: 请输入完整键名或前缀进行搜索</p>';
}

function addOrUpdateKV() {
    const key = document.getElementById('kvKey').value.trim();
    const value = document.getElementById('kvValue').value;
    
    if (!key) return alert('请输入键名');
    
    fetch('/kv/set', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({key, value})
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            alert('✅ 保存成功');
            document.getElementById('kvKey').value = '';
            document.getElementById('kvValue').value = '';
        } else {
            alert('❌ 保存失败: ' + data.message);
        }
    })
    .catch(err => alert('❌ 请求失败: ' + err.message));
}

function loadStatus() {
    const statusDiv = document.getElementById('statusContent');
    statusDiv.innerHTML = '<p>加载中...</p>';
    
    // 这里可以添加状态查询API
    statusDiv.innerHTML = `
        <div class="status-item">
            <label>SQLite 数据库</label>
            <span>midb.sqlite</span>
        </div>
        <div class="status-item">
            <label>LevelDB 数据库</label>
            <span>leveldb_data/</span>
        </div>
        <div class="status-item">
            <label>HTTP 服务</label>
            <span>端口 8080</span>
        </div>
    `;
}
