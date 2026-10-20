import { money } from '../orderStatus.js'

export default function Catalog({ products, onAdd }) {
  return (
    <>
      <h2>Catálogo</h2>
      <ul className="grid">
        {products.map((product) => (
          <li key={product.id} className="card">
            <span className="sku">{product.id}</span>
            <h3>{product.name}</h3>
            <p className="price">{money.format(product.price)}</p>
            <p className={product.available > 0 ? 'stock' : 'stock out'}>
              {product.available > 0 ? `${product.available} disponibles` : 'Agotado'}
            </p>
            <button onClick={() => onAdd(product.id)} disabled={product.available === 0}>
              Agregar
            </button>
          </li>
        ))}
      </ul>
    </>
  )
}
