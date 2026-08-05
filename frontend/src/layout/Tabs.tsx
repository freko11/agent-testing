export interface TabItem {
  id: string
  label: string
}

interface TabsProps {
  tabs: TabItem[]
  activeId: string
  onChange: (id: string) => void
}

/** Generic tab-bar nav — the dashboard's Trade/Orders/Notifications sections render as
 * `hidden` panels rather than being unmounted on switch, so in-progress state (a typed
 * symbol, a pending refresh) survives moving to another tab and back. */
function Tabs({ tabs, activeId, onChange }: TabsProps) {
  return (
    <div className="app-tabs" role="tablist">
      {tabs.map((tab) => (
        <button
          key={tab.id}
          type="button"
          role="tab"
          aria-selected={tab.id === activeId}
          className={`app-tab${tab.id === activeId ? ' app-tab--active' : ''}`}
          onClick={() => onChange(tab.id)}
        >
          {tab.label}
        </button>
      ))}
    </div>
  )
}

export default Tabs
