-- This is a Lua scratchpad which allows you to change
-- the layout in the center of the screen dynamically.
--
-- Additionally to all JVx components, there are three
-- global variables which you can use:
--
--  * panel: The UIPanel in the center of the screen.
--  * layout: The UIFormLayout for that panel.
--  * stub(): A function which creates a simple stub
--            component.
--
-- The panel/layout will be updated dynamically.

panel:add(stub(), layout:getConstraints(0, 0))
panel:add(stub(), layout:getConstraints(1, 1))
panel:add(UIButton.new("Click"), layout:getConstraints(-1, -1))
