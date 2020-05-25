var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode');
var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode');

var Class = function(c) {
    this.c = c;
    this.class = this;
}
var voido = new Class('V');
var byte = new Class('B');
var char = new Class('C');
var double = new Class('D');
var float = new Class('F');
var int = new Class('I');
var long = new Class('J');
var short = new Class('S');
var boolean = new Class('Z');

var ClassName = function(clsName) {
    this.clsName = clsName;
}
ClassName.of = function(clsName) {
    return new ClassName(clsName);
}
ClassName.prototype.getBytecodeName = function() {
    return this.clsName.split('.').join('/');
}
ClassName.prototype.getName = function() {
    return this.clsName.split('/').join('.');
}

var DescHelper = function() {}
/**
 {@link #toDesc(Object)}をMethodのDescriptor用に使えるようにしたもの。
 下手なクラスをここに入れようとするとまずいので確信がない限りClassNameで入れるべき。

 @param returnType {@link ClassName}型か、{@link String}型か、{@link Class}型で目的のMethodの返り値の型を指定する。
 @param rawDesc {@link ClassName}型か、{@link String}型か、{@link Class}型でMethodの引数たちの型を指定する。
 @throws IllegalArgumentException 引数に{@link ClassName}型か、{@link String}型か、{@link Class}型以外が入ったら投げられる。
 @return Javaバイトコードで扱われる形の文字列に変換されたDescriptor。
 */
DescHelper.toDescMethod = function(returnType) {
    var sb = '(';
    Array.prototype.slice.call(arguments, 1).forEach(function(o) {
        if (o!==null)
            sb += DescHelper.toDesc(o);
    });
    sb += ')';
    sb += DescHelper.toDesc(returnType);
    return sb;
}
/**
 {@link Class#forName}とか{@link Class#getCanonicalName()}したりするとまだ読み込まれてなかったりしてまずいので安全策。
 下手なクラスをここに入れようとするとまずいので確信がない限りClassNameで入れるべき。

 @param raw {@link ClassName}型か、{@link String}型か、{@link Class}型でASM用の文字列に変換したいクラスを指定する。
 @throws IllegalArgumentException {@param raw}に{@link String}型か、{@link String}型か、{@link Class}型以外が入ったら投げられる。
 @return Javaバイトコードで扱われる形の文字列に変換されたクラス。
 */
DescHelper.toDesc = function(raw) {
    if (raw instanceof Class) {
        return raw.c;
    } else if (typeof(raw) === 'string') {
        var desc = raw;
        desc = ClassName.of(desc).getBytecodeName();
        desc = desc.matches("L.+;") ? desc : "L"+desc+";";
        return desc;
    } else if (raw instanceof ClassName) {
        var desc = raw.getBytecodeName();
        desc = desc.matches("L.+;") ? desc : "L"+desc+";";
        return desc;
    }
    throw new Error("IllegalArgumentException");
}

var VisitorHelper = function() {}
VisitorHelper.insnToList = function(insn) {
    var list = [];
    for (var i = 0; i < insn.size(); i++)
        list.push(insn.get(i));
    return list;
}

function sizeHookTransform(name) {
    return function(method) {
        /*
            public EntitySize getSize(Pose poseIn) {
                return GulliverHooks.fireEntityGetSizeEvent(this.type.getSize(), this, poseIn);
            }
         */
        /*
           L0
            LINENUMBER 3091 L0
            ALOAD 0
            GETFIELD net/minecraft/entity/Entity.type : Lnet/minecraft/entity/EntityType;
            INVOKEVIRTUAL net/minecraft/entity/EntityType.getSize ()Lnet/minecraft/entity/EntitySize;
            ALOAD 0
            ALOAD 1
            INVOKESTATIC net/teamfruit/gulliver/event/GulliverHooks.fireEntityGetSizeEvent (Lnet/minecraft/entity/EntitySize;Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Pose;)Lnet/minecraft/entity/EntitySize;
            ARETURN
           L1
            LOCALVARIABLE this Lnet/minecraft/entity/Entity; L0 L1 0
            LOCALVARIABLE poseIn Lnet/minecraft/entity/Pose; L0 L1 1
            MAXSTACK = 3
            MAXLOCALS = 2
         */
        var marker = ASMAPI.findFirstInstruction(method, Opcodes.ARETURN);
        var insertion = new InsnList();
        insertion.add(new VarInsnNode(Opcodes.ALOAD, 0));
        insertion.add(new VarInsnNode(Opcodes.ALOAD, 1));
        insertion.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ClassName.of("net.teamfruit.gulliver.event.GulliverHooks").getBytecodeName(), name, DescHelper.toDescMethod(ClassName.of("net.minecraft.entity.EntitySize"), ClassName.of("net.minecraft.entity.EntitySize"), ClassName.of("net.minecraft.entity.Entity"), ClassName.of("net.minecraft.entity.Pose")), false));
        method.instructions.insertBefore(marker, insertion);
        return method;
    };
}

function initializeCoreMod() {
    return {
        'EntityTransform': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.entity.Entity',
                'methodName': 'func_213305_a',
                'methodDesc': DescHelper.toDescMethod(ClassName.of("net.minecraft.entity.EntitySize"), ClassName.of("net.minecraft.entity.Pose"))
            },
            'transformer': sizeHookTransform("fireEntityGetSizeEvent")
        },
        'PlayerTransform': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.entity.player.PlayerEntity',
                'methodName': 'func_213305_a',
                'methodDesc': DescHelper.toDescMethod(ClassName.of("net.minecraft.entity.EntitySize"), ClassName.of("net.minecraft.entity.Pose"))
            },
            'transformer': sizeHookTransform("firePlayerGetSizeEvent")
        }
    };
}