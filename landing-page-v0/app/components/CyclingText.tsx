'use client'

import { useState, useEffect } from 'react'

export function CyclingText() {
  const words = ['care gaps', 'HEDIS scores', 'Star Ratings', 'quality bonuses']
  const [currentIndex, setCurrentIndex] = useState(0)
  const [isVisible, setIsVisible] = useState(true)

  useEffect(() => {
    const interval = setInterval(() => {
      setIsVisible(false)
      setTimeout(() => {
        setCurrentIndex((prev) => (prev + 1) % words.length)
        setIsVisible(true)
      }, 300)
    }, 3000)

    return () => clearInterval(interval)
  }, [])

  return (
    <span
      className={`inline-block transition-all duration-300 text-accent ${
        isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-2'
      }`}
    >
      {words[currentIndex]}
    </span>
  )
}
