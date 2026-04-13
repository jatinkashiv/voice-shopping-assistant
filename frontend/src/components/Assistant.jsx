import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { sendVoiceCommand, getItems, clearItems } from '../api/api'
import './AssistantCSS.css'

function Assistant() {
  const navigate = useNavigate()

  const [status, setStatus] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [listening, setListening] = useState(false)
  const [items, setItems] = useState([])

  const recognitionRef = useRef(null)

  const loadItems = async () => {
    try {
      setLoading(true)
      const data = await getItems()
      setItems(data)
      setError('')
    } catch {
      setError('Failed to load items')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadItems()
  }, [])

  useEffect(() => {
    const SpeechRecognition =
      window.SpeechRecognition || window.webkitSpeechRecognition

    if (!SpeechRecognition) return

    const recognition = new SpeechRecognition()
    recognition.lang = 'en-US'
    recognition.continuous = true

    recognition.onresult = async (event) => {
      const command = event.results[event.results.length - 1][0].transcript
      setStatus(`You said: "${command}"`)
      setLoading(true)

      try {
        const data = await sendVoiceCommand(command)
        setStatus(prev => prev + `\nAdded: ${data.name}`)
        setError('')
        loadItems()
      } catch {
        setError('Failed to send command')
      } finally {
        setLoading(false)
      }
    }

    recognitionRef.current = recognition
  }, [])

  const toggleListening = () => {
    const recognition = recognitionRef.current
    if (!recognition) return

    if (listening) {
      recognition.stop()
      setListening(false)
      setStatus('Stopped listening')
    } else {
      recognition.start()
      setListening(true)
      setStatus('Listening...')
    }
  }

  const clearAll = async () => {
    try {
      setLoading(true)
      await clearItems()
      loadItems()
      setStatus('All items cleared')
      setError('')
    } catch {
      setError('Failed to clear items')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="assistant-wrapper">
      <button className="btn back-btn" onClick={() => navigate('/')}>
        ← Back
      </button>

      <div className="assistant-center">
        <div className={`water-round-container ${listening ? 'active' : ''}`}>
          <div className="water-wave1"></div>
          <div className="water-wave2"></div>
          <div className="water-wave3"></div>
        </div>
      </div>

      <button
        className="btn mic-btn"
        onClick={toggleListening}
        disabled={loading}
      >
        {listening ? 'Stop Listening' : 'Put Your Voice'}
      </button>

      <button
        className="btn clear-btn"
        onClick={clearAll}
        disabled={loading}
      >
        Clear All
      </button>

      {loading && <p className="loading">Processing...</p>}
      {error && <p className="error">{error}</p>}

      <pre className="status-text">{status}</pre>

      <ul className="item-list">
        {items.length === 0 && !loading && <li>No items yet</li>}
        {items.map(item => (
          <li key={item.id}>
            {item.name} ({item.quantity} {item.unit})
          </li>
        ))}
      </ul>
    </div>
  )
}

export default Assistant
