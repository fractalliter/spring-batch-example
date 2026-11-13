import { useEffect, useRef, useState } from 'react'
import { createRoot } from 'react-dom/client'
import './style.css'

function Icon({ name, size = 18, strokeWidth = 1.7 }) {
  const paths = {
    users: <><circle cx="9" cy="8" r="3"/><path d="M3 20v-2a6 6 0 0 1 12 0v2M17 5a3 3 0 0 1 0 6m1 3a5 5 0 0 1 3 4v2"/></>,
    spark: <path d="m12 2 2.3 7.7L22 12l-7.7 2.3L12 22l-2.3-7.7L2 12l7.7-2.3L12 2Z"/>,
    arrow: <path d="M4 12h16m-6-6 6 6-6 6"/>,
    plug: <><path d="M9 3v6m6-6v6M7 9h10v3a5 5 0 0 1-10 0z"/><path d="M12 17v4"/></>,
    check: <path d="m4 12 5 5L20 6"/>,
    x: <path d="M6 6l12 12M18 6 6 18"/>,
    clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
  }
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name]}</svg>
}

// Trạng thái tổng của trang: null=đang tải, 'offline', 'running', 'ready'
function pageState(dashboard, loadError) {
  if (loadError && !dashboard) return 'offline'
  if (!dashboard) return 'loading'
  if (dashboard.running) return 'running'
  return 'ready'
}

const STATE_META = {
  loading: { label: 'Đang tải…', icon: 'clock', cls: 'is-loading' },
  offline: { label: 'Mất kết nối', icon: 'plug', cls: 'is-offline' },
  running: { label: 'Đang chạy', icon: 'spark', cls: 'is-running' },
  ready: { label: 'Sẵn sàng', icon: 'check', cls: 'is-ready' },
}

function App() {
  const [dashboard, setDashboard] = useState(null)
  const [loadError, setLoadError] = useState('')
  const [action, setAction] = useState({ busy: false, message: '', ok: false })
  const [tab, setTab] = useState('week')
  const [winners, setWinners] = useState([])
  const [celebrate, setCelebrate] = useState(null)
  const [logStep, setLogStep] = useState(null)
  const seenWinner = { current: undefined }

  async function refresh() {
    try {
      const response = await fetch('/api/dashboard')
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      setDashboard(await response.json())
      setLoadError('')
    } catch (error) {
      setLoadError(error.message || 'Không tải được dữ liệu')
    }
    try {
      const response = await fetch('/api/winners')
      if (!response.ok) return
      const list = await response.json()
      const fresh = list[0]
      if (seenWinner.current !== undefined && fresh && fresh.id !== seenWinner.current) {
        setCelebrate(fresh)
      }
      if (fresh) seenWinner.current = fresh.id
      setWinners(list)
    } catch {
      /* danh sách winner thiếu vẫn dùng được phần còn lại */
    }
  }

  useEffect(() => {
    refresh()
    const timer = window.setInterval(() => { if (!document.hidden) refresh() }, 5000)
    return () => window.clearInterval(timer)
  }, [])

  useEffect(() => {
    if (!celebrate) return
    const onKey = (event) => { if (event.key === 'Escape') setCelebrate(null) }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [celebrate])

  useEffect(() => {
    if (!logStep) return
    const onKey = (event) => { if (event.key === 'Escape') setLogStep(null) }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [logStep])

  async function startJob() {
    setAction({ busy: true, message: 'Đang gửi yêu cầu…', ok: false })
    try {
      const response = await fetch('/api/jobs/executions', {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{}',
      })
      if (!response.ok) throw new Error(response.status === 409 ? 'Job đang chạy rồi.' : `Không chạy được (HTTP ${response.status}).`)
      const result = await response.json()
      setAction({ busy: false, message: `Đã gửi job #${result.executionId}.`, ok: true })
      refresh()
    } catch (error) {
      setAction({ busy: false, message: error.message, ok: false })
      refresh()
    }
  }

  const state = pageState(dashboard, loadError)
  const meta = STATE_META[state]
  const runs = dashboard?.executions ?? []
  const latest = runs[0] ?? null
  const hasData = dashboard !== null

  return <div className="page">
    <header className="top">
      <div className="brand"><span className="brand-mark"><span/><span/><span/></span>WINNERS</div>
      <div className={`state-pill ${meta.cls}`} role="status" aria-live="polite">
        <Icon name={meta.icon} size={13}/>{meta.label}
      </div>
    </header>

    <main className="content">
      {/* ===== Trạng thái tổng ===== */}
      {state === 'loading' && <section className="card hero" aria-busy="true">
        <div className="skeleton skeleton-title"/><div className="skeleton skeleton-line"/>
        <div className="skeleton skeleton-line short"/><div className="skeleton skeleton-block"/>
        <p className="hero-note">Đang lấy dữ liệu từ máy chủ…</p>
      </section>}

      {state === 'offline' && <section className="card hero is-error">
        <span className="hero-glyph"><Icon name="plug" size={30}/></span>
        <h1>Không kết nối được máy chủ</h1>
        <p>Backend chưa chạy hoặc mất mạng. Lỗi: {loadError}</p>
        <button type="button" className="button primary" onClick={refresh}>Thử lại</button>
      </section>}

      {hasData && <section className="card hero">
        <h1>Người thắng tuần này</h1>
        <p className="hero-sub">
          {dashboard.running
            ? 'Hệ thống đang chọn, kết quả sẽ hiện sau ít phút.'
            : latest
              ? 'Lần chọn gần nhất đã xong. Xem chi tiết bên dưới.'
              : 'Chưa có lần chọn nào trong tuần. Bạn có thể chọn ngay hoặc đợi Chủ Nhật 23:59.'}
        </p>
        <div className="hero-actions">
          <button type="button" className="button primary" onClick={startJob}
            disabled={dashboard.running || action.busy}>
            {action.busy ? 'Đang gửi…' : dashboard.running ? 'Đang chạy…' : 'Chọn ngay'}
          </button>
          <button type="button" className="button" onClick={refresh} title="Làm mới">Làm mới</button>
        </div>
        <div className="action-note" role="status" aria-live="polite">
          {action.message && <span className={action.ok ? 'note-ok' : 'note-fail'}>{action.message}</span>}
          {loadError && <span className="note-fail">Cập nhật định kỳ bị gián đoạn: {loadError}</span>}
          {!action.message && !loadError && <span className="note-idle">Tự cập nhật mỗi 5 giây · lịch chạy {dashboard.schedule}</span>}
        </div>
      </section>}

      {/* ===== Tabs ===== */}
      {hasData && <nav className="tabs" aria-label="Khung xem">
        <button type="button" role="tab" aria-selected={tab === 'week'} className={`tab ${tab === 'week' ? 'selected' : ''}`} onClick={() => setTab('week')}>Tuần này</button>
        <button type="button" role="tab" aria-selected={tab === 'history'} className={`tab ${tab === 'history' ? 'selected' : ''}`} onClick={() => setTab('history')}>Lịch sử</button>
        <button type="button" role="tab" aria-selected={tab === 'winners'} className={`tab ${tab === 'winners' ? 'selected' : ''}`} onClick={() => setTab('winners')}>Người thắng{winners.length > 0 ? ` · ${winners.length}` : ''}</button>
      </nav>}

      {hasData && tab === 'winners' && <section className="card" aria-label="Danh sách người thắng">
        {winners.length === 0
          ? <div className="blank">
              <span className="hero-glyph"><Icon name="spark" size={30}/></span>
              <h2>Chưa có ai thắng</h2>
              <p>Danh sách sẽ hiện sau lần chọn đầu tiên.</p>
            </div>
          : <ol className="history winners-list">
              {winners.map((winner) => <li key={winner.id} className="history-row winner-row">
                <span className="winner-name">{winner.name}</span>
                <span className="winner-meta">@{winner.username} · {winner.city ?? '—'} · {winner.companyName ?? '—'}</span>
                <time>{winner.wonAt ? new Date(winner.wonAt).toLocaleString('vi-VN') : ''}</time>
              </li>)}
            </ol>}
      </section>}

      {hasData && tab === 'week' && <section className="card" aria-label="Kết quả gần nhất">
        {latest
          ? <div className="latest">
              <div className="latest-head">
                <span className={`status status-${latest.status.toLowerCase()}`}>
                  <i className="status-shape" aria-hidden="true"/>{latest.status === 'COMPLETED' ? 'Hoàn tất' : latest.status === 'FAILED' ? 'Thất bại' : 'Đang chạy'}
                </span>
                <time>{latest.startedAt ? new Date(latest.startedAt).toLocaleString('vi-VN') : 'Đang chờ'}</time>
              </div>
              <ul className="facts">
                <li><strong>{dashboard.usersCount}</strong><span>người dùng</span></li>
                <li><strong>{dashboard.transactionsCount}</strong><span>giao dịch</span></li>
                <li><strong>&gt; {dashboard.threshold}</strong><span>ngưỡng thắng</span></li>
              </ul>
              <ol className="steps">
                {latest.steps.map((step) => <li key={step.name}>
                  <button type="button" className="step-open" onClick={() => setLogStep(step.name)}
                    title={`Xem log của ${step.name}`}>
                    <span className="step-name">{step.name}</span>
                    <span className={`status status-${step.status.toLowerCase()}`}><i className="status-shape" aria-hidden="true"/>{step.status === 'COMPLETED' ? 'xong' : step.status === 'FAILED' ? 'lỗi' : 'đang chạy'}</span>
                    <span className="step-counts">đọc {step.readCount} · ghi {step.writeCount}{step.skipCount > 0 ? ` · bỏ qua ${step.skipCount}` : ''}</span>
                  </button>
                </li>)}
              </ol>
            </div>
          : <div className="blank">
              <span className="hero-glyph"><Icon name="spark" size={30}/></span>
              <h2>Chưa có kết quả nào</h2>
              <p>Nhấn “Chọn ngay” ở trên để chạy lần đầu.</p>
            </div>}
      </section>}

      {hasData && tab === 'history' && <section className="card" aria-label="Lịch sử chạy">
        {runs.length === 0
          ? <div className="blank">
              <span className="hero-glyph"><Icon name="clock" size={30}/></span>
              <h2>Chưa có lần chạy nào</h2>
              <p>Lịch sử sẽ xuất hiện sau lần chạy đầu tiên.</p>
            </div>
          : <ul className="history">
              {runs.map((run) => <li key={run.id} className="history-row">
                <span className="run-id">#{run.id}</span>
                <span className={`status status-${run.status.toLowerCase()}`}><i className="status-shape" aria-hidden="true"/>{run.status === 'COMPLETED' ? 'Hoàn tất' : run.status === 'FAILED' ? 'Thất bại' : 'Đang chạy'}</span>
                <time>{run.startedAt ? new Date(run.startedAt).toLocaleString('vi-VN') : 'Đang chờ'}</time>
              </li>)}
            </ul>}
      </section>}

      <footer className="foot">Spring Batch · PostgreSQL · Kafka</footer>
    </main>

    {logStep && <LogStream step={logStep} onClose={() => setLogStep(null)} />}

    {celebrate && <div className="overlay" onClick={() => setCelebrate(null)}>
      <div className="dialog" role="dialog" aria-modal="true" aria-label="Chúc mừng người thắng"
        onClick={(event) => event.stopPropagation()}>
        <div className="dialog-badge"><Icon name="spark" size={26}/></div>
        <h2 className="dialog-title">Chúc mừng!</h2>
        <p className="dialog-name">{celebrate.name}</p>
        <dl className="dialog-facts">
          <div><dt>Username</dt><dd>@{celebrate.username}</dd></div>
          <div><dt>Điện thoại</dt><dd>{celebrate.phone}</dd></div>
          <div><dt>Website</dt><dd>{celebrate.website}</dd></div>
          <div><dt>Công ty</dt><dd>{celebrate.companyName}</dd></div>
          <div><dt>Thành phố</dt><dd>{celebrate.city}</dd></div>
          <div><dt>Thời điểm</dt><dd>{new Date(celebrate.wonAt).toLocaleString('vi-VN')}</dd></div>
        </dl>
        {winners.length > 1 && <div className="dialog-list-wrap">
          <div className="dialog-list-title">10 người thắng gần nhất</div>
          <ol className="history winners-list dialog-list">
            {winners.map((winner) => <li key={winner.id} className="history-row winner-row">
              <span className="winner-name">{winner.name}</span>
              <span className="winner-meta">@{winner.username} · {winner.city ?? '—'}</span>
              <time>{winner.wonAt ? new Date(winner.wonAt).toLocaleString('vi-VN') : ''}</time>
            </li>)}
          </ol>
        </div>}
        <button type="button" className="button primary dialog-close" autoFocus
          onClick={() => setCelebrate(null)}>Đóng</button>
      </div>
    </div>}
  </div>
}

createRoot(document.getElementById('root')).render(<App />)

// ===== Panel stream log theo step: nguồn sự kiện /api/logs/stream?step=… =====
function LogStream({ step, onClose }) {
  const [lines, setLines] = useState([])
  const [connected, setConnected] = useState(false)
  const boxRef = useRef(null)

  useEffect(() => {
    const source = new EventSource(`/api/logs/stream?step=${encodeURIComponent(step)}`)
    source.onopen = () => setConnected(true)
    source.onmessage = (event) => setLines((prev) => [...prev.slice(-400), event.data])
    source.onerror = () => setConnected(false)
    return () => source.close()
  }, [step])

  useEffect(() => {
    if (boxRef.current) boxRef.current.scrollTop = boxRef.current.scrollHeight
  }, [lines])

  return <div className="overlay" onClick={onClose}>
    <div className="dialog log-dialog" role="dialog" aria-modal="true" aria-label={`Log của ${step}`}
      onClick={(event) => event.stopPropagation()}>
      <div className="log-head">
        <div className="log-title">
          <span className="log-step">{step}</span>
          <span className={`log-live ${connected ? 'on' : ''}`} aria-hidden="true"/>
          <span className="log-live-label">{connected ? 'trực tiếp' : 'mất kết nối'}</span>
        </div>
        <p className="log-note">Log từ máy chủ, lọc theo step · nhấn Esc để đóng</p>
      </div>
      <pre className="log-box" ref={boxRef}>
        {lines.length === 0
          ? <span className="log-empty">Đang chờ log của {step}… Hãy chạy job để thấy dòng mới.</span>
          : lines.map((line, index) => <span key={index} className="log-line">{line}</span>)}
      </pre>
      <button type="button" className="button primary dialog-close" autoFocus onClick={onClose}>Đóng</button>
    </div>
  </div>
}
