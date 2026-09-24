function AuthInput({ label, type, name, value, onChange, autoComplete }) {
  return (
    <>
      <label htmlFor={name}>{label}</label>
      <input
        id={name}
        type={type}
        name={name}
        value={value}
        onChange={onChange}
        autoComplete={autoComplete || (type === 'password' ? 'current-password' : name)}
        required
      />
    </>
  )
}

export default AuthInput
