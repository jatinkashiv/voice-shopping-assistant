import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getItems } from '../api/api'
 
const BASE_URL = 'http://localhost:8080/api'

function DataView() {
  const navigate = useNavigate()
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

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

  const deleteItem = async (id) => {
    try {
      await fetch(`${BASE_URL}/items/${id}`, {
        method: 'DELETE'
      })
      loadItems()
    } catch {
      setError('Delete failed')
    }
  }

  return (
    <div className="assistant-wrapper">
      <button className="btn back-btn" onClick={() => navigate('/')}>
        ← Back
      </button>

      <h2>Shopping Cart</h2>

      {loading && <p className="loading">Loading...</p>}
      {error && <p className="error">{error}</p>}

      <ul className="item-list">
        {items.length === 0 && !loading && <li>No items yet</li>}

        {items.map(item => (
          <li key={item.id}>
            {item.name} ({item.quantity} {item.unit})
            <button
              className="delete-btn"
              onClick={() => deleteItem(item.id)}
            >
              Delete
            </button>
          </li>
        ))}
      </ul>
    </div>
  )
}

export default DataView
