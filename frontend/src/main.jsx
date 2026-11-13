import { createRoot } from 'react-dom/client'
import './style.css'

function App() {
  return <main className="p-6 text-xl font-semibold">React 19 + Tailwind CSS v4</main>
}

createRoot(document.getElementById('root')).render(<App />)
