import { Routes, Route, useNavigate } from 'react-router-dom'
import DataView from './components/DataView'
import Assistant from './components/Assistant'
import './App.css'

function Home() {
  const navigate = useNavigate()

  return (
    <>
      <div className="home-bg">
        <div className="home-content">
          <h1 className="hover-text">
            {"Voice Command Shopping Assistant".split("").map((c, i) => (
              <span key={i}>{c === " " ? "\u00A0" : c}</span>
            ))}
          </h1>

          <h2>What you want:</h2>

          <div className="button-div">
            <button className="btn" onClick={() => navigate('/assistant')}>
              Shop now
            </button>

            <button className="btn" onClick={() => navigate('/data')}>
              View Cart Data
            </button>

          </div>
        </div>
      </div>
    </>
  )
}

function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/assistant" element={<Assistant />} />
      <Route path="/data" element={<DataView />} />
    </Routes>
  )
}

export default App
