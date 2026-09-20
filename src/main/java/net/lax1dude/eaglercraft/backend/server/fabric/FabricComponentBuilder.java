/*
 * EaglerXServer - Fabric platform bridge for Minecraft 26.2
 *
 * Licensed under the MIT License.
 */

package net.lax1dude.eaglercraft.backend.server.fabric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformComponentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

class FabricComponentBuilder implements IPlatformComponentBuilder {

	interface IAppendCallback {
		void append(Component comp);
	}

	@Override
	public IBuilderComponentText<Object> buildTextComponent() {
		return new BuilderTextRoot();
	}

	@Override
	public IBuilderComponentTranslation<Object> buildTranslationComponent() {
		return new BuilderTranslationRoot();
	}

	// ------------------------------------------------------------------

	static class BuilderStyle<ParentType> implements IBuilderStyle<ParentType> {

		private final ParentType parent;
		EnumChatColor color;
		boolean bold;
		boolean italic;
		boolean strikethrough;
		boolean underline;
		boolean obfuscated;

		BuilderStyle(ParentType parent) {
			this.parent = parent;
		}

		@Override
		public ParentType end() {
			return parent;
		}

		@Override
		public IBuilderStyle<ParentType> color(EnumChatColor color) {
			this.color = color;
			return this;
		}

		@Override
		public IBuilderStyle<ParentType> bold(boolean bold) {
			this.bold = bold;
			return this;
		}

		@Override
		public IBuilderStyle<ParentType> italic(boolean italic) {
			this.italic = italic;
			return this;
		}

		@Override
		public IBuilderStyle<ParentType> strikethrough(boolean strikethrough) {
			this.strikethrough = strikethrough;
			return this;
		}

		@Override
		public IBuilderStyle<ParentType> underline(boolean underline) {
			this.underline = underline;
			return this;
		}

		@Override
		public IBuilderStyle<ParentType> obfuscated(boolean obfuscated) {
			this.obfuscated = obfuscated;
			return this;
		}

		void applyTo(MutableComponent ret) {
			Style style = Style.EMPTY;
			if (color != null) {
				style = style.withColor(ChatFormatting.values()[color.ordinal()]);
			}
			if (bold) {
				style = style.withBold(true);
			}
			if (italic) {
				style = style.withItalic(true);
			}
			if (strikethrough) {
				style = style.withStrikethrough(true);
			}
			if (underline) {
				style = style.withUnderlined(true);
			}
			if (obfuscated) {
				style = style.withObfuscated(true);
			}
			ret.setStyle(ret.getStyle().applyTo(style));
		}

	}

	static class BuilderClick<ParentType> implements IBuilderClickEvent<ParentType> {

		private final ParentType parent;
		EnumClickAction action;
		String value;

		BuilderClick(ParentType parent) {
			this.parent = parent;
		}

		@Override
		public ParentType end() {
			return parent;
		}

		@Override
		public IBuilderClickEvent<ParentType> clickAction(EnumClickAction action) {
			this.action = action;
			return this;
		}

		@Override
		public IBuilderClickEvent<ParentType> clickValue(String value) {
			this.value = value;
			return this;
		}

		void applyTo(MutableComponent ret) {
			if (action != null) {
				ClickEvent event = null;
				switch (action) {
				case OPEN_URL:
					try {
						event = new ClickEvent.OpenUrl(new java.net.URI(value != null ? value : ""));
					} catch (Throwable t) {
					}
					break;
				case OPEN_FILE:
					event = new ClickEvent.OpenFile(value != null ? value : "");
					break;
				case RUN_COMMAND:
					event = new ClickEvent.RunCommand(value != null ? value : "");
					break;
				case SUGGEST_COMMAND:
					event = new ClickEvent.SuggestCommand(value != null ? value : "");
					break;
				case CHANGE_PAGE:
					try {
						event = new ClickEvent.ChangePage(Integer.parseInt(value != null ? value : "0"));
					} catch (Throwable t) {
					}
					break;
				case COPY_TO_CLIPBOARD:
					event = new ClickEvent.CopyToClipboard(value != null ? value : "");
					break;
				}
				if (event != null) {
					ret.setStyle(ret.getStyle().withClickEvent(event));
				}
			}
		}

	}

	static class BuilderHover<ParentType> implements IBuilderHoverEvent<ParentType>, IAppendCallback {

		private final ParentType parent;
		EnumHoverAction action;
		List<Component> buildChildren;

		BuilderHover(ParentType parent) {
			this.parent = parent;
		}

		@Override
		public ParentType end() {
			return parent;
		}

		@Override
		public IBuilderHoverEvent<ParentType> hoverAction(EnumHoverAction action) {
			this.action = action;
			return this;
		}

		@Override
		public IBuilderComponentText<IBuilderHoverEvent<ParentType>> appendTextContent() {
			return new BuilderTextChild<>(this);
		}

		@Override
		public IBuilderComponentTranslation<IBuilderHoverEvent<ParentType>> appendTranslationContent() {
			return new BuilderTranslationChild<>(this);
		}

		@Override
		public void append(Component comp) {
			if (buildChildren == null) {
				buildChildren = new ArrayList<>(4);
			}
			buildChildren.add(comp);
		}

		void applyTo(MutableComponent ret) {
			if (action == EnumHoverAction.SHOW_TEXT && buildChildren != null) {
				MutableComponent content = Component.empty();
				for (Component child : buildChildren) {
					content.append(child);
				}
				ret.setStyle(ret.getStyle().withHoverEvent(new HoverEvent.ShowText(content)));
			}
		}

	}

	// ------------------------------------------------------------------

	static abstract class BuilderTextBase<ParentType> implements IBuilderComponentText<ParentType>, IAppendCallback {

		BuilderStyle<IBuilderComponentText<ParentType>> style;
		BuilderClick<IBuilderComponentText<ParentType>> click;
		BuilderHover<IBuilderComponentText<ParentType>> hover;
		String text;
		String insertion;
		List<Component> buildChildren;

		@Override
		public IBuilderStyle<IBuilderComponentText<ParentType>> beginStyle() {
			return style = new BuilderStyle<>(this);
		}

		@Override
		public IBuilderClickEvent<IBuilderComponentText<ParentType>> beginClickEvent() {
			return click = new BuilderClick<>(this);
		}

		@Override
		public IBuilderHoverEvent<IBuilderComponentText<ParentType>> beginHoverEvent() {
			return hover = new BuilderHover<>(this);
		}

		@Override
		public IBuilderComponentText<IBuilderComponentText<ParentType>> appendTextComponent() {
			return new BuilderTextChild<>(this);
		}

		@Override
		public IBuilderComponentTranslation<IBuilderComponentText<ParentType>> appendTranslationComponent() {
			return new BuilderTranslationChild<>(this);
		}

		@Override
		public IBuilderComponentText<ParentType> insertion(String txt) {
			this.insertion = txt;
			return this;
		}

		@Override
		public IBuilderComponentText<ParentType> text(String txt) {
			this.text = txt;
			return this;
		}

		@Override
		public void append(Component comp) {
			if (buildChildren == null) {
				buildChildren = new ArrayList<>(4);
			}
			buildChildren.add(comp);
		}

		protected Component build() {
			MutableComponent ret = text != null ? Component.literal(text) : Component.empty();
			if (insertion != null) {
				ret.setStyle(ret.getStyle().withInsertion(insertion));
			}
			if (style != null) {
				style.applyTo(ret);
			}
			if (click != null) {
				click.applyTo(ret);
			}
			if (hover != null) {
				hover.applyTo(ret);
			}
			if (buildChildren != null) {
				for (Component child : buildChildren) {
					ret.append(child);
				}
			}
			return ret;
		}

	}

	static class BuilderTextRoot extends BuilderTextBase<Object> {

		@Override
		public Object end() {
			return build();
		}

	}

	static class BuilderTextChild<ParentType> extends BuilderTextBase<ParentType> {

		private final IAppendCallback parent;

		BuilderTextChild(IAppendCallback parent) {
			this.parent = parent;
		}

		@Override
		@SuppressWarnings("unchecked")
		public ParentType end() {
			parent.append(build());
			return (ParentType) parent;
		}

	}

	// ------------------------------------------------------------------

	static abstract class BuilderTranslationBase<ParentType>
			implements IBuilderComponentTranslation<ParentType>, IAppendCallback {

		BuilderStyle<IBuilderComponentTranslation<ParentType>> style;
		BuilderClick<IBuilderComponentTranslation<ParentType>> click;
		BuilderHover<IBuilderComponentTranslation<ParentType>> hover;
		String translation;
		String insertion;
		List<Component> buildChildren;
		List<Object> args;

		@Override
		public IBuilderStyle<IBuilderComponentTranslation<ParentType>> beginStyle() {
			return style = new BuilderStyle<>(this);
		}

		@Override
		public IBuilderClickEvent<IBuilderComponentTranslation<ParentType>> beginClickEvent() {
			return click = new BuilderClick<>(this);
		}

		@Override
		public IBuilderHoverEvent<IBuilderComponentTranslation<ParentType>> beginHoverEvent() {
			return hover = new BuilderHover<>(this);
		}

		@Override
		public IBuilderComponentText<IBuilderComponentTranslation<ParentType>> appendTextComponent() {
			return new BuilderTextChild<>(this);
		}

		@Override
		public IBuilderComponentTranslation<IBuilderComponentTranslation<ParentType>> appendTranslationComponent() {
			return new BuilderTranslationChild<>(this);
		}

		@Override
		public IBuilderComponentTranslation<ParentType> insertion(String txt) {
			this.insertion = txt;
			return this;
		}

		@Override
		public IBuilderComponentTranslation<ParentType> translation(String txt) {
			this.translation = txt;
			return this;
		}

		@Override
		public IBuilderComponentTranslation<ParentType> stringArgs(Object... args) {
			this.args = new ArrayList<>(args.length);
			for (int i = 0; i < args.length; ++i) {
				this.args.add(Component.literal(Objects.toString(args[i])));
			}
			return this;
		}

		@Override
		public IBuilderComponentTranslation<ParentType> stringArgs(List<Object> args) {
			int size = args.size();
			this.args = new ArrayList<>(size);
			for (int i = 0; i < size; ++i) {
				this.args.add(Component.literal(Objects.toString(args.get(i))));
			}
			return this;
		}

		@SuppressWarnings("unchecked")
		@Override
		public IBuilderComponentTranslation<ParentType> componentArgs(Object... args) {
			this.args = (List<Object>) (Object) Arrays.asList(args);
			return this;
		}

		@SuppressWarnings("unchecked")
		@Override
		public IBuilderComponentTranslation<ParentType> componentArgs(List<Object> args) {
			this.args = args;
			return this;
		}

		@Override
		public IBuilderComponentTranslationArgs<IBuilderComponentTranslation<ParentType>> args() {
			return new BuilderTranslationArgs<>(this);
		}

		@Override
		public void append(Component comp) {
			if (buildChildren == null) {
				buildChildren = new ArrayList<>(4);
			}
			buildChildren.add(comp);
		}

		protected Component build() {
			if (translation == null) {
				throw new IllegalStateException("No translation key specified");
			}
			Object[] argsArr = args != null ? args.toArray() : new Object[0];
			MutableComponent ret = Component.translatable(translation, argsArr);
			if (insertion != null) {
				ret.setStyle(ret.getStyle().withInsertion(insertion));
			}
			if (style != null) {
				style.applyTo(ret);
			}
			if (click != null) {
				click.applyTo(ret);
			}
			if (hover != null) {
				hover.applyTo(ret);
			}
			if (buildChildren != null) {
				for (Component child : buildChildren) {
					ret.append(child);
				}
			}
			return ret;
		}

	}

	static class BuilderTranslationRoot extends BuilderTranslationBase<Object> {

		@Override
		public Object end() {
			return build();
		}

	}

	static class BuilderTranslationChild<ParentType> extends BuilderTranslationBase<ParentType> {

		private final IAppendCallback parent;

		BuilderTranslationChild(IAppendCallback parent) {
			this.parent = parent;
		}

		@Override
		@SuppressWarnings("unchecked")
		public ParentType end() {
			parent.append(build());
			return (ParentType) parent;
		}

	}

	static class BuilderTranslationArgs<ParentType>
			implements IBuilderComponentTranslationArgs<ParentType>, IAppendCallback {

		private final ParentType parent;
		private List<Object> args;

		BuilderTranslationArgs(ParentType parent) {
			this.parent = parent;
		}

		@Override
		public IBuilderComponentText<IBuilderComponentTranslationArgs<ParentType>> textArg() {
			return new BuilderTextChild<>(this);
		}

		@Override
		public IBuilderComponentTranslation<IBuilderComponentTranslationArgs<ParentType>> translateArg() {
			return new BuilderTranslationChild<>(this);
		}

		@Override
		public IBuilderComponentTranslationArgs<ParentType> rawArg(Object component) {
			append((Component) component);
			return this;
		}

		@Override
		public void append(Component comp) {
			if (args == null) {
				args = new ArrayList<>(4);
			}
			args.add(comp);
		}

		@Override
		public ParentType end() {
			if (args != null) {
				((BuilderTranslationBase<?>) parent).componentArgs(args);
			}
			return parent;
		}

	}

}